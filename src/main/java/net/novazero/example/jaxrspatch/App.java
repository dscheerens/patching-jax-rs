package net.novazero.example.jaxrspatch;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import net.novazero.example.jaxrspatch.patchsupport.JsonPatchReader;
import net.novazero.example.jaxrspatch.patchsupport.PartialJsonObjectPatchReader;
import net.novazero.example.jaxrspatch.resources.CustomerResource;

public class App {

	public static void main(String[] arguments) throws Exception {
		
		ResourceConfig config = new ResourceConfig();
		
		config.register(new ServiceBinder());
		config.register(JacksonFeature.class);
		config.register(PartialJsonObjectPatchReader.class);
		config.register(JsonPatchReader.class);
		config.register(CustomerDataLoader.class);
		config.register(CustomerResource.class);
		
		URI baseUri = UriBuilder.fromUri("http://localhost/").port(8080).build();
		Server server = JettyHttpContainerFactory.createServer(baseUri, config);
		
		try {
			server.start();
			server.join();
		} finally {
			server.stop();
		}
	}

}
