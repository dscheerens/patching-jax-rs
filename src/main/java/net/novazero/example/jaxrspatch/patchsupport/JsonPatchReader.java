package net.novazero.example.jaxrspatch.patchsupport;

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

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == ObjectPatch.class && MediaTypes.APPLICATION_JSON_PATCH_TYPE.isCompatible(mediaType);
	}

	@Override
	public ObjectPatch readFrom(Class<ObjectPatch> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
		
		JsonNode patch = OBJECT_MAPPER.readTree(entityStream);
		
		return new JsonObjectPatch(OBJECT_MAPPER, patch);
	}
	
}
