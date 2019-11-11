package org.zerofour.swagger2.converter.oas3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

public class JSON extends LinkedHashMap<String,Object> {
	private static ObjectMapper om;

	private static class Deserializer extends JsonDeserializer<Object> {
		@Override
		public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			ObjectCodec objectCodec = p.getCodec();
			JsonNode jsonNode = objectCodec.readTree(p);

			switch(jsonNode.getNodeType()) {
				case NULL:
				case MISSING:
					return null;
				case POJO:
				case OBJECT:
					return objectCodec.treeToValue(jsonNode, JSON.class);
				case ARRAY:
					return objectCodec.treeToValue(jsonNode, List.class);
				case BINARY:
				case STRING:
					return jsonNode.asText();
				case NUMBER:
					return jsonNode.asInt();
				case BOOLEAN:
					return jsonNode.asBoolean();
			}
			return null;
		}
	}

	static {
		om = new ObjectMapper();
		om.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		om.registerModule(new SimpleModule()
			.addDeserializer(Object.class, new Deserializer()));
	}

	public static JSON readValue(String str) throws IOException {
		return om.readValue(str, JSON.class);
	}

	public static JSON readValue(Object obj) throws IOException {
		return om.convertValue(obj, JSON.class);
	}

	public <T> T convert(Class<T> clazz) {
		return om.convertValue(this, clazz);
	}

	public String writeValue() throws JsonProcessingException {
		return om.writeValueAsString(this);
	}
}
