Patching JAX-RS
===============

For those who are not familiar with the term JAX-RS; it an API for building RESTful web services.
The most recent release is [JAX-RS 2.0](https://jax-rs-spec.java.net/) and is covered in [JSR 339](https://jcp.org/en/jsr/detail?id=339).
Some of the most popular implementations of the API include:

* [Apache CXF](https://cxf.apache.org/)
* [Jersey](https://jersey.java.net/)
* [RESTeasy](http://resteasy.jboss.org/)

While I believe that JAX-RS API is quite complete, I find it lacking one feature in particular: support for the HTTP PATCH method.
If you attempt to use the `@PATCH` annotation on any of the methods of your resource classes, you'll soon discover that JAX-RS does not define this annotation.
According to [some](https://blogs.oracle.com/theaquarium/entry/using_http_patch_with_jax) the `@PATCH` annotation was not included in JAX-RS 2.0 because the HTTP PATCH method is not that widely understood and used.
Whether or not this is the actual reason, let's assume so and first find out what it is used for, before exploring how to use it in JAX-RS applications.

Introducing the PATCH method
----------------------------

The HTTP `PATCH` method is defined in [RFC 5789](https://tools.ietf.org/html/rfc5789) as an extension of [HTTP 1.1](https://tools.ietf.org/html/rfc2616), and provides a dedicated request method to apply partial modifications to a resource.
Note that the `PUT` method could not be used for this purpose since its definition specifies that is used to completely overwrite a resource.
Now that it clear what the `PATCH` method is used for, let's see why one would use it anyway.

Suppose you already have built a REST API and a new customer comes that wishes to use your API to connect your services to their application.
Obviously their data model differs from yours and they are only interested in querying and updating a subset of your data.
Now what happens if they want to update the name of a project (an entity exposed by your web service)?
Without the `PATCH` method, the customer needs to first perform a `GET` request to obtain the latest version and then a `PUT` request for the same project with an updated name property.
While this approach works it has some problems:

* There is considerable overhead, since it requires two HTTP requests.
Also, depending on the used format used to exchange the entity, it requires you to deserialize and serialize the entity.
Furthermore more data is transferred than necessary, since the entire project state is transferred, while the only information that needs to be conveyed to your web service is the new project name.
* Chances are that concurrency eventually becomes an issue.
The project state might actually be changed in between of the `GET` and `PUT` requests, resulting in lost modifications.

Both issues are addressed if your web service instead offers support for partial modification using the `PATCH` method.
In this scenario the customer only needs to make a single `PATCH` request for the project in question that only contains the new project name.
If your web service accepts JSON for example, one way to update the project name could be using the following request body:

```
{
  "name": "New project name"
}
```

It does not get much simpler than this.

Existing solutions
------------------
The scenario presented above is not that far away from a real JAX-RS based application that I have been working on.
So when I tried to implement patch support in the application I quickly discovered JAX-RS does not offer any.
Instead of trying to reinvent the wheel I first searched for solutions provided by others.
Apart from some details on how to implement a `@PATCH` annotation, which is quite easy, I found only one solution that actually explains how to deal with partial modifications in JAX-RS.
This is the [Transparent PATCH Support in JAX-RS 2.0](https://dzone.com/articles/transparent-patch-support-jax) article written by Gerard Davison.
In summary the approach presented in this article suggests using a `ReaderInterceptor` to internally first call the `@GET` annotated method in the same resource class to obtain the current state of the object that needs to be updated.
Then a JSON patch is applied to that object to obtain the new modified state of the object.
While this solution works for certain cases, it has the following limitations and drawbacks:

* It requires a '@GET' annotated method without any parameters to be present in the same resource class.
It is quite reasonable to assume that whenever your resource class exposes a `PATCH` endpoint there is also a `GET` endpoint available.
However, the main problem lies in the fact that the `@GET` method cannot take any parameters.
This makes the solution impossible to use when you need an `id` parameter for example.
* The solution has a dependency on the `MessageBodyWorkers` class provided by Jersey and is therefore not agnostic of the JAX-RS implementation.
* The `PatchReader` class is annotated `@PATCH`, which in turn is annotated with `@NameBinding`.
To me this feel like a violation of the single responsibility principle.

Supporting partial modifications in JAX-RS
------------------------------------------
Since none of the solutions I found was really usable, I started working on a better one, which I will describe below.
First lets get the `@PATCH` annotation out of the way.
As said before it turns out the `@PATCH` annotation is quite easy to implement.
Simply by looking at the source of existing HTTP method annotations, like `@GET`, it becomes clear how a `@PATCH` annotation can be implemented.

```java
package com.example;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.HttpMethod;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH")
@Documented
public @interface PATCH {

}
```

Since the `@HttpMethod` annotation is treated as a transitive annotation, the `@PATCH` implementation above can simply be used for methods in resource classes that process `PATCH` requests.
Note that the `@PATCH` annotation by itself does not yet enable us to perform partial modifications.
In fact the only thing it adds is the ability to handle incoming `PATCH` requests.
So in order to enable partial resource modification we need something more.
Let's start by introducing an interface that enables us to perform an update to a certain resource:

```java
package com.example;

public interface ObjectPatch {

  <T> T apply(T target) throws ObjectPatchException;
  
}
```

The only method provided by the `ObjectPatch` is an `apply` method that takes an object as input and which returns the same object after the modifications have been applied to the input object.
Note that the input object is returned for convenience, but this is not strictly necessary since the input object is modified.
Using generics we can ensure that the return type of the method is the same as the input type of the target parameter.
Also, something might go wrong while the modifications are applied, hence the `apply` method may throw our own defined `ObjectPatchException`.
The following example demonstrates how the `ObjectPatch` will be used in a resource class:

```java
package com.example;

// Imports...

@Path("/customers")
public class CustomerResource {
  
  private final CustomerDao customerDao; // Set via DI.

  // GET, POST, PUT, DELETE methods...
  
  @PATCH
  @Path("/{id}")
  public Customer patchCustomer(@PathParam("id") long id, ObjectPatch patch) {
    // Lookup the customer by id.
    Customer customer = customerDao.get(id);

    // Apply partial modifications.
    patch.apply(customer);

    // Persist modified customer.
    customerDao.update(customer);

    return customer;
  }
}
```

Of course the example above is incomplete, but it outlines how to process partial modifications for a resource.
Apart from the fact that the example is incomplete, it also would not work since no `ObjectPatch` implementations are available.
If you would run the example, and tried to perform a `PATCH` request you will be treated with a nice HTTP 500 - internal server error.
This is because there is no way for the used JAX-RS implementation to provide an `ObjectPatch` instance.
Therefore we need to tell the JAX-RS implementation how to create instances of the `ObjectPatch` interface.
Fortunately we can do this without committing to a specific JAX-RS implementation.
Via the `MessageBodyReader` interface JAX-RS allows you to define your own method of reading a specific Java type from an `InputStream`.

To create our own `MessageBodyReader` implementation for the `ObjectPatch` interface, we first need to decide on the format of the patch that the reader accepts.
We'll start simple by accepting partial JSON objects.
Suppose our `Customer` entity has several properties, including a `name` property, then the following request would update the name of customer with id 1:

```
PATCH /customers/1 HTTP/1.1
HOST: localhost:8080
content-type: application/json
content-length: 22

{ "name": "John Doe" }
```

In order to support the request above the `MessageBodyReader` needs to be able to parse JSON.
The [Jackson](https://github.com/FasterXML/jackson) library will do nice for this.
Our `MessageBodyReader` then looks like this:

```java
package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PartialJsonObjectPatchReader implements MessageBodyReader<ObjectPatch> {
  
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  
  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
    MediaType mediaType) {
    return ObjectPatch.class == type && MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType);
  }
  
  @Override
  public ObjectPatch readFrom(Class<ObjectPatch> type, Type genericType, Annotation[] annotations,
    MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
    throws IOException, WebApplicationException {
    
    JsonNode patch = OBJECT_MAPPER.readTree(entityStream);
    
    return new PartialJsonObjectPatch(OBJECT_MAPPER, patch);
  }
  
}
```

The `isReadable` method is called for each message body parameter to check whether the `MessageBodyReader` can create an instance of the specified Java type for given media type.
In our case this is true for the `ObjectPatch` interface and `application/json` media type.
Conversion of the message body to an `ObjectPatch` instance happens in the `readFrom` method.
Here Jackson is used to first read a JSON tree from the provided `InputStream` of the request body.
Then we return a new instance of `PartialJsonObjectPatch`, which is instantiated with the JSON tree that was read from the request body.
`PartialJsonObjectPatch` implements the `ObjectPatch` interface and the implementation is given below.

```java
package com.example;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class PartialJsonObjectPatch implements ObjectPatch {
  
  private final ObjectMapper objectMapper;
  
  private final JsonNode patch;
  
  public PartialJsonObjectPatch(ObjectMapper objectMapper, JsonNode patch) {
    this.objectMapper = objectMapper;
    this.patch = patch;
  }

  @Override
  public <T> T apply(T target) throws ObjectPatchException {
    
    ObjectReader reader = objectMapper.readerForUpdating(target);
    
    try {
      return reader.readValue(patch);
    } catch (IOException e) {
      throw new ObjectPatchException(e);
    }
  }
  
}
```

The magic of the `PartialJsonObjectPatch` class happens of course in the `apply` method.
Jackson is again used to obtain an `ObjectReader` instance that can be used to update the target object given some JSON source.
In the `try-catch`-block the reader is used to update the target object using the JSON tree that was passed to the constructor of the `PartialJsonObjectPatch` instance.
Any `IOException` that might be thrown by Jackson is caught and wrapped in a `ObjectPatchException`.

And thats all!
The solution presented above is really all that is necessary to support partial resource modifications.
Note that I have not explained how to register the `PartialJsonObjectPatchReader` class with the JAX-RS implementation.
This is something you will need to do, otherwise the JAX-RS implementation is not aware of your `MessageBodyReader` and the solution does not work.
It is out of scope to explain how to register `MessageBodyReader` classes as this differs per JAX-RS implementation.
however, At the end of this article you will find a link to a fully working example project that uses Jersey and you can look at the source to discover how `MessageBodyWriter` classes are registered using Jersey.

Adding JSON patch (RFC 6902) support
------------------------------------

While the partial resource modification support that is outlined above works like a charm, it has some limitations.
With the partial JSON object approach you can only replace the value of certain properties with another value.
But what if you want to insert an element into a property that contains and array or want to update the value of property from a nested object?
This again would only be possible by first obtaining the current state of these properties, applying the modifications and then sending the updated properties back to the web service.
A `GET` request would then still be necessary prior to making the `PATCH` request.
To address this problem the JSON patch specification ([RFC 6902](https://tools.ietf.org/html/rfc6902)) was drafted.

Let's assume our `Customer` entity (introduced in the previous section) also has a `phoneNumber` property, which contains an array of strings.
Using JSON patch, inserting a phone number in the second position of this array for customer with id 2, would look like this:

```
PATCH /customers/2 HTTP/1.1
HOST: localhost:8080
content-type: application/json+patch
content-length: 60

[{ "op": "add", "path": "/phoneNumbers/1", "value": "666" }]
```

Note that RFC 6902 uses a new media type: `application/json+patch`.
Our application would not support the request above, since it uses a different media type.
Also, ignoring the different media type, the current implementation expects an object as root of the JSON tree, rather than an array.
JSON patch uses an array in which each element describes an operation that needs to be applied in order to perform the modifications.
In the example above there is one operation: inserting the value `"666"` at the second position of the `phoneNumbers` array.

How do we get our application to support JSON patch requests like the one shown above?
If a library exists that implements RFC 6902 then this will get us a long way.
Fortunately this is the case and I have chosen [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch) for this as it also uses Jackson 2.x.
Let's start by creating a new `MessageBodyReader` for JSON patch requests.

```java
package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonPatchReader implements MessageBodyReader<ObjectPatch> {
  
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final MediaType APPLICATION_JSON_PATCH_TYPE = new MediaType("application", "json+patch");
  
  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
    MediaType mediaType) {
    return ObjectPatch.class == type && APPLICATION_JSON_PATCH_TYPE.isCompatible(mediaType);
  }
  
  @Override
  public ObjectPatch readFrom(Class<ObjectPatch> type, Type genericType, Annotation[] annotations,
    MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
    throws IOException, WebApplicationException {
    
    JsonNode patch = OBJECT_MAPPER.readTree(entityStream);
    
    return new JsonObjectPatch(OBJECT_MAPPER, patch);
  }
  
}
```

As you can see the implementation is almost identical to that of the `PartialJsonObjectPatchReader` class presented earlier.
The only differences are that it only accepts requests with a media type of `application/json+patch` and that it returns an `JsonObjectPatch` instance rather than a `PartialJsonObjectPatch` instance.
Since there is nothing special that is new, we'll continue with the `JsonObjectPatch` implementation.

```java
package com.example;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.zjsonpatch.JsonPatch;

public class JsonObjectPatch implements ObjectPatch {
  
  private final ObjectMapper objectMapper;
  
  private final JsonNode patch;
  
  public JsonObjectPatch(ObjectMapper objectMapper, JsonNode patch) {
    this.objectMapper = objectMapper;
    this.patch = patch;
  }

  @Override
  public <T> T apply(T target) throws ObjectPatchException {
    
    JsonNode source = objectMapper.valueToTree(target);
    
    JsonNode result;
    try {
      result = JsonPatch.apply(patch, source);
    } catch (NullPointerException e) {
      throw new ObjectPatchException(e);
    }
    
    ObjectReader reader = objectMapper.readerForUpdating(target);
    
    try {
      return reader.readValue(result);
    } catch (IOException e) {
      throw new ObjectPatchException(e);
    }
  }
  
}
```

As before, the `JsonObjectPatch` class does not differ much from the `PartialJsonObjectPatch` class.
In fact the only difference can be found in the `apply` method.
Instead of applying the `patch` JSON tree directly on the target object, zjsonpatch is used to apply the `patch` JSON tree on a JSON tree of the target object.
This is placed in a `try-catch`-block since zjsonpatch may throw a `NullPointerException` (probably due to a flaw in the implementation) if the patch JSON tree does not conform to the RFC 6902 specification.
Finally the resulting JSON tree is applied to the target object as before using an `ObjectReader`.

Again you will need to register the `JsonPatchReader` with the JAX-RS implementation before it is recognized.
After doing so your application will be able process JSON patch requests! Hooray!

Conclusion
----------

This article has presented a way to implement support for partial resource modification in JAX-RS based web services.
First it has shown how to be able to support HTTP PATCH requests and then outlined a solution of processing partial resource modifications via partial JSON objects and JSON patch ([RFC 6902](https://tools.ietf.org/html/rfc6902)).
The solution is designed to be agnostic of the JAX-RS implementation and in theory should work with any library/framework that implements JAX-RS 2.0.
It also shows how your application can be extended to support other data formats to apply partial modifications.
For example support for JSON merge patch, [RFC 7386](https://tools.ietf.org/html/rfc7386), or the XML equivalent of JSON patch, [RFC 5261](http://tools.ietf.org/html/rfc5261), could easily be supported as well using the same concepts.

To demonstrate the solution presented in this article, I have made a complete example application which can be found at the following GitHub repository: [https://github.com/dscheerens/patching-jax-rs](https://github.com/dscheerens/patching-jax-rs).
The example application uses an embedded Jetty server in combination with Jersey to provide a simple web service for maintaining a collection of customers.

As a final note let me address one drawback of the presented solution.
Contrary to the to `POST` and `PUT` request methods, it is no longer possible to directly obtain an instance of your entity from the request body as a parameter of the method in your resource class that processes the request.
Hence you lose the ability to add annotations to the entity instance.
This can be a problem for example if you make use of *Bean Validation* ([JSR 303](https://jcp.org/aboutJava/communityprocess/final/jsr303/index.html)).
One way to deal with this problem is to obtain an reference of the `Validator` instance and call its `validate` method manually inside the method that processes the partial resource modification.
