package net.novazero.example.jaxrspatch.patchsupport;

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