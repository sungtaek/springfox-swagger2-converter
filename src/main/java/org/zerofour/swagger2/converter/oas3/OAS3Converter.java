package org.zerofour.swagger2.converter.oas3;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import java.math.BigDecimal;
import java.util.*;

public class OAS3Converter {

	public OpenAPI convertOpenAPI(JSON oas2) {
		// check version
		if(oas2.get("swagger") == null
			|| !((String)oas2.get("swagger")).equals("2.0")) {
			return null;
		}

		OpenAPI openAPI = new OpenAPI();

		// info
		if(oas2.get("info") != null) {
			openAPI.setInfo(((JSON)oas2.get("info")).convert(Info.class));
		}
		// servers
		openAPI.setServers(Arrays.asList(new Server().url((String)oas2.get("basePath"))));
		// paths
		openAPI.setPaths(convertPaths((JSON)oas2.get("paths")));
		// components
		openAPI.setComponents(convertComponents((JSON)oas2.get("definitions"),
			(List)oas2.get("produces"),
			(JSON)oas2.get("responses"),
			(JSON)oas2.get("parameters"),
			(JSON)oas2.get("securityDefinitions")));
		// security
		if(oas2.get("security") != null) {
			for(JSON oas2Security: (List<JSON>)oas2.get("security")) {
				openAPI.addSecurityItem(oas2Security.convert(SecurityRequirement.class));
			}
		}
		// tags
		if(oas2.get("tags") != null) {
			for(JSON oas2Tag: (List<JSON>)oas2.get("tags")) {
				openAPI.addTagsItem(oas2Tag.convert(Tag.class));
			}
		}
		// externalDocs
		if(oas2.get("externalDocs") != null) {
			openAPI.setExternalDocs(((JSON)oas2.get("externalDocs")).convert(ExternalDocumentation.class));
		}

		return openAPI;
	}

	private Paths convertPaths(JSON oas2Paths) {
		Paths paths = new Paths();
		for(Map.Entry<String,Object> e: oas2Paths.entrySet()) {
			JSON oas2Path = (JSON)e.getValue();
			PathItem pathItem = new PathItem();
			if(oas2Path.get("$ref") != null) {
				pathItem.$ref(convertRef((String)oas2Path.get("$ref")));
			}
			else {
				if(oas2Path.get("get") != null) {
					pathItem.get(convertOperation((JSON)oas2Path.get("get")));
				}
				if(oas2Path.get("put") != null) {
					pathItem.put(convertOperation((JSON)oas2Path.get("put")));
				}
				if(oas2Path.get("post") != null) {
					pathItem.post(convertOperation((JSON)oas2Path.get("post")));
				}
				if(oas2Path.get("delete") != null) {
					pathItem.delete(convertOperation((JSON)oas2Path.get("delete")));
				}
				if(oas2Path.get("options") != null) {
					pathItem.options(convertOperation((JSON)oas2Path.get("options")));
				}
			}
			paths.addPathItem(e.getKey(), pathItem);
		}
		return paths;
	}

	private Components convertComponents(JSON oas2Definitions,
	                                     List<String> oas2Produces,
	                                     JSON oas2Responses,
	                                     JSON oas2Parameters,
	                                     JSON oas2SecurityDefinitions) {
		Components components = new Components();

		// schemas
		if(oas2Definitions != null) {
			for(Map.Entry<String,Object> e: oas2Definitions.entrySet()) {
				components.addSchemas(e.getKey(), convertSchema((JSON)e.getValue()));
			}
		}
		// responses
		if(oas2Responses != null) {
			for(Map.Entry<String,Object> e: oas2Responses.entrySet()) {
				components.addResponses(e.getKey(), convertResponse(oas2Produces, (JSON)e.getValue()));
			}
		}
		// parameters
		if(oas2Parameters != null) {
			for(Map.Entry<String,Object> e: oas2Parameters.entrySet()) {
				Parameter parameter = convertParameter((JSON)e.getValue());
				if(parameter != null) {
					components.addParameters(e.getKey(), parameter);
				}
			}
		}
		// securitySchemes
		if(oas2SecurityDefinitions != null) {
			for(Map.Entry<String,Object> e: oas2SecurityDefinitions.entrySet()) {
				components.addSecuritySchemes(e.getKey(), convertSecurityScheme((JSON)e.getValue()));
			}
		}

		return components;
	}

	private Operation convertOperation(JSON oas2Operation) {
		Operation operation = new Operation();

		// tags
		operation.setTags((List)oas2Operation.get("tags"));
		// summary
		operation.setSummary((String)oas2Operation.get("summary"));
		// description
		operation.setDescription((String)oas2Operation.get("description"));
		// externalDocs
		if(oas2Operation.get("externalDocs") != null) {
			operation.setExternalDocs(((JSON)oas2Operation.get("externalDocs")).convert(ExternalDocumentation.class));
		}
		// operationId
		operation.setOperationId((String)oas2Operation.get("operationId"));
		// parameters
		List<JSON> oas2Parameters = (List)oas2Operation.get("parameters");
		if(oas2Parameters != null && oas2Parameters.size() > 0) {
			List<Parameter> parameters = new ArrayList<>();
			for(JSON oas2Parameter: oas2Parameters) {
				Parameter parameter = convertParameter(oas2Parameter);
				if(parameter != null) {
					parameters.add(parameter);
				}
			}
			operation.setParameters(parameters);
		}
		// requestBody
		operation.setRequestBody(convertRequestBody((List)oas2Operation.get("consumes"), oas2Parameters));
		// responses
		if(oas2Operation.get("responses") != null) {
			operation.setResponses(new ApiResponses());
			for(Map.Entry<String,Object> e: ((JSON)oas2Operation.get("responses")).entrySet()) {
				operation.getResponses().addApiResponse(e.getKey(),
					convertResponse((List)oas2Operation.get("produces"), (JSON)e.getValue()));
			}
		}
		// deprecated
		operation.setDeprecated((Boolean)oas2Operation.get("deprecated"));
		// security
		if(oas2Operation.get("security") != null) {
			for(Object securityItem: (List)oas2Operation.get("security")) {
				operation.addSecurityItem(((JSON)securityItem).convert(SecurityRequirement.class));
			}
		}

		return operation;
	}

	private Parameter convertParameter(JSON oas2Parameter) {
		Parameter parameter = null;

		if(oas2Parameter.get("$ref") != null) {
			parameter = new Parameter();
			parameter.set$ref(convertRef((String)oas2Parameter.get("$ref")));
			return parameter;
		}

		String in = (String)oas2Parameter.get("in");
		if(in == null)
			return null;
		if(!in.equals("query") && !in.equals("header") && !in.equals("path")) {
			return null;
		}

		parameter = new Parameter();

		// name
		parameter.setName((String)oas2Parameter.get("name"));
		// in
		parameter.setIn(in);
		// description
		parameter.setDescription((String)oas2Parameter.get("description"));
		// required
		parameter.setRequired((Boolean)oas2Parameter.get("required"));
		// allowEmptyValue
		parameter.setAllowEmptyValue((Boolean)oas2Parameter.get("allowEmptyValue"));
		// style
		if(in.equals("query")) {
			parameter.setStyle(Parameter.StyleEnum.FORM);
		}
		else {
			parameter.setStyle(Parameter.StyleEnum.SIMPLE);
		}
		// schema
		parameter.setSchema(convertSchemaFromParameter(oas2Parameter));

		return parameter;
	}

	private RequestBody convertRequestBody(List<String> oas2Consumes, List<JSON> oas2Parameters) {
		RequestBody requestBody = null;

		List<JSON> formParameters = new ArrayList<>();
		JSON bodyParameter = null;

		for(JSON oas2Parameter: oas2Parameters) {
			String in = (String)oas2Parameter.get("in");
			if(in != null && in.equals("formData")) {
				formParameters.add(oas2Parameter);
			}
			else if(in != null && in.equals("body")) {
				bodyParameter = oas2Parameter;
			}
		}

		if(oas2Consumes == null) {
			oas2Consumes = new ArrayList<>();
		}

		if(bodyParameter != null) {
			requestBody = new RequestBody();

			if(bodyParameter.get("$ref") != null) {
				requestBody.set$ref(convertRef((String)bodyParameter.get("$ref")));
				return requestBody;
			}

			// description
			requestBody.setDescription((String)bodyParameter.get("description"));
			// required
			requestBody.setRequired((Boolean)bodyParameter.get("required"));
			// content
			requestBody.setContent(new Content());
			MediaType mediaType = new MediaType()
				.schema(convertSchema((JSON)bodyParameter.get("schema")));
			if(oas2Consumes.size() == 0) {
				oas2Consumes.add("application/json");
			}
			for(String oas2Consume: oas2Consumes) {
				requestBody.getContent().addMediaType(oas2Consume, mediaType);
			}
		}
		else if(formParameters.size() > 0) {
			requestBody = new RequestBody();

			List<String> required = getRequiredOfFormParameters(formParameters);

			// required
			requestBody.setRequired((required.size()>0)?true:null);
			// content
			requestBody.setContent(new Content());
			MediaType mediaType = new MediaType()
				.schema(new Schema()
					.type("object")
					.properties(new HashMap<>())
					.required(required));
			for(JSON formParameter: formParameters) {
				mediaType.getSchema().getProperties().put(formParameter.get("name"),
					convertSchemaFromParameter(formParameter));
				String contentType = getContentTypeOfFormParameter(formParameter);
				if(contentType != null) {
					mediaType.addEncoding((String)formParameter.get("name"),
						new Encoding().contentType(contentType));
				}
			}
			if(oas2Consumes.size() == 0) {
				oas2Consumes.add(getDefaultContentTypeOfFormParameters(formParameters));
			}
			for(String oas2Consume: oas2Consumes) {
				requestBody.getContent().addMediaType(oas2Consume, mediaType);
			}
		}

		return requestBody;
	}

	private List<String> getRequiredOfFormParameters(List<JSON> oas2FormParameters) {
		List<String> required = new ArrayList<>();
		for(JSON formParameter: oas2FormParameters) {
			if(formParameter.get("required") != null && (Boolean)formParameter.get("required")) {
				required.add((String)formParameter.get("name"));
			}
		}
		return required;
	}

	private String getContentTypeOfFormParameter(JSON oas2FormParameter) {
		if(oas2FormParameter.get("schema") != null) {
			JSON schema = (JSON)oas2FormParameter.get("schema");
			if("object".equals(schema.get("type"))
				|| schema.get("$ref") != null) {
				return "application/json";
			}
		}
		return null;
	}

	private String getDefaultContentTypeOfFormParameters(List<JSON> oas2FormParameters) {
		for(JSON formParameter: oas2FormParameters) {
			if(formParameter.get("type") != null && formParameter.get("type").equals("file")) {
				return "multipart/form-data";
			}
			if(formParameter.get("schema") != null) {
				return "multipart/form-data";
			}
		}
		return "application/x-www-form-urlencoded";
	}

	private ApiResponse convertResponse(List<String> oas2Produces, JSON oas2Response) {
		ApiResponse apiResponse = new ApiResponse();

		if (oas2Response.get("$ref") != null) {
			apiResponse.set$ref(convertRef((String) oas2Response.get("$ref")));
		} else {
			// description
			apiResponse.setDescription((String) oas2Response.get("description"));
			// headers
			if(oas2Response.get("headers") != null) {
				apiResponse.setHeaders(new HashMap<>());
				for(Map.Entry<String,Object> e: ((JSON)oas2Response.get("headers")).entrySet()) {
					apiResponse.getHeaders().put(e.getKey(),
						convertHeader((JSON)e.getValue()));
				}
			}
			// content
			if(oas2Response.get("schema") != null) {
				apiResponse.setContent(new Content());
				MediaType mediaType = new MediaType()
					.schema(convertSchema((JSON)oas2Response.get("schema")));
				if(oas2Produces == null) {
					oas2Produces = new ArrayList<>();
				}
				if(oas2Produces.size() == 0) {
					oas2Produces.add("application/json");
				}
				for(String oas2Produce: oas2Produces) {
					apiResponse.getContent().addMediaType(oas2Produce, mediaType);
				}
			}
		}

		return apiResponse;
	}

	private Header convertHeader(JSON oas2Header) {
		Header header =new Header();
		if(oas2Header.get("$ref") != null) {
			header.set$ref(convertRef((String)oas2Header.get("$ref")));
		}
		else {
			// description
			header.setDescription((String)oas2Header.get("description"));
			// schema
			header.setSchema(convertSchemaFromParameter(oas2Header));
		}
		return header;
	}

	private void convertRefOfSchema(JSON oas2Schema) {
		if(oas2Schema.get("$ref") != null) {
			oas2Schema.put("$ref", convertRef((String)oas2Schema.get("$ref")));
		}
		if(oas2Schema.get("items") != null) {
			convertRefOfSchema((JSON)oas2Schema.get("items"));
		}
		if(oas2Schema.get("properties") != null) {
			for(Object property: ((JSON)oas2Schema.get("properties")).values()) {
				convertRefOfSchema((JSON)property);
			}
		}
	}

	private Schema convertSchema(JSON oas2Schema) {
		Schema schema = null;

		convertRefOfSchema(oas2Schema);

		if(oas2Schema.get("$ref") != null) {
			schema = new Schema().$ref((String)oas2Schema.get("$ref"));
		}
		else {
			String type = (String)oas2Schema.get("type");
			if("string".equals(type)) {
				schema = oas2Schema.convert(StringSchema.class);
			}
			else if("boolean".equals(type)) {
				schema = oas2Schema.convert(BooleanSchema.class);
			}
			else if("array".equals(type)) {
				schema = oas2Schema.convert(ArraySchema.class);
				if(oas2Schema.get("items") != null) {
					((ArraySchema)schema).setItems(convertSchema((JSON)oas2Schema.get("items")));
				}
			}
			else if("file".equals(type)) {
				schema = oas2Schema.convert(FileSchema.class);
				schema.setType("string");
				schema.setFormat("binary");
			}
			else if("object".equals(type)) {
				schema = oas2Schema.convert(ObjectSchema.class);
				if(oas2Schema.get("properties") != null) {
					JSON oas2Properties = (JSON)oas2Schema.get("properties");
					Map<String,Schema> properties = new HashMap<>();
					for(Map.Entry<String,Object> e: oas2Properties.entrySet()) {
						properties.put(e.getKey(), convertSchema((JSON)e.getValue()));
					}
					((ObjectSchema)schema).setProperties(properties);
				}
			}
			// TODO: need to check to use another Schema type
			else {
				schema = oas2Schema.convert(Schema.class);
			}
		}
		return schema;
	}

	private Schema convertSchemaFromParameter(JSON oas2Parameter) {
		Schema schema = null;

		if(oas2Parameter.get("schema") != null) {
			schema = convertSchema((JSON)oas2Parameter.get("schema"));
		}
		else {
			String type = (String)oas2Parameter.get("type");
			if("array".equals(type)) {
				schema = new ArraySchema();
				if(oas2Parameter.get("items") != null) {
					((ArraySchema)schema).setItems(convertSchema((JSON)oas2Parameter.get("items")));
				}
			}
			else if("file".equals(type)) {
				schema = new FileSchema();
			}
			else {
				schema = new Schema()
					.type(type)
					.format((String)oas2Parameter.get("format"));
			}
			schema.setMaximum((BigDecimal) oas2Parameter.get("maximum"));
			schema.setExclusiveMaximum((Boolean) oas2Parameter.get("exclusiveMaximum"));
			schema.setMinimum((BigDecimal) oas2Parameter.get("minimum"));
			schema.setExclusiveMaximum((Boolean) oas2Parameter.get("exclusiveMinimum"));
			schema.setMaxLength((Integer) oas2Parameter.get("maxLength"));
			schema.setMinLength((Integer) oas2Parameter.get("minLength"));
			schema.setPattern((String) oas2Parameter.get("pattern"));
			schema.setMaxItems((Integer) oas2Parameter.get("maxItems"));
			schema.setMinItems((Integer) oas2Parameter.get("minItems"));
			schema.setUniqueItems((Boolean) oas2Parameter.get("uniqueItems"));
			schema.setEnum((List) oas2Parameter.get("enum"));
			schema.setDefault(oas2Parameter.get("default"));
		}

		return schema;
	}

	private SecurityScheme convertSecurityScheme(JSON oas2SecurityScheme) {
		SecurityScheme securityScheme = new SecurityScheme();

		if(oas2SecurityScheme.get("$ref") != null) {
			securityScheme.$ref(convertRef((String)oas2SecurityScheme.get("$ref")));
		}
		else {
			String type = (String)oas2SecurityScheme.get("type");
			if("basic".equals(type)) {
				securityScheme.setType(SecurityScheme.Type.HTTP);
				securityScheme.setScheme("basic");
			}
			else if("apiKey".equals(type)) {
				securityScheme.setType(SecurityScheme.Type.APIKEY);
				securityScheme.setName((String)oas2SecurityScheme.get("name"));
				String in = (String)oas2SecurityScheme.get("in");
				if("query".equals(in)) {
					securityScheme.setIn(SecurityScheme.In.QUERY);
				}
				else if("header".equals(in)) {
					securityScheme.setIn(SecurityScheme.In.HEADER);
				}
			}
			else if("oauth2".equals(type)) {
				securityScheme.setType(SecurityScheme.Type.OAUTH2);
				securityScheme.setFlows(new OAuthFlows());
				Scopes scopes = null;
				if(oas2SecurityScheme.get("scopes") != null) {
					scopes = ((JSON)oas2SecurityScheme.get("scopes")).convert(Scopes.class);
				}
				String authorizationUrl = (String)oas2SecurityScheme.get("authorizationUrl");
				String tokenUrl = (String)oas2SecurityScheme.get("tokenUrl");
				String flow = (String)oas2SecurityScheme.get("flow");
				if("implicit".equals(flow)) {
					securityScheme.getFlows()
						.setImplicit(new OAuthFlow()
							.authorizationUrl(authorizationUrl)
							.scopes(scopes));
				}
				else if("password".equals(flow)) {
					securityScheme.getFlows()
						.setPassword(new OAuthFlow()
							.tokenUrl(tokenUrl)
							.scopes(scopes));
				}
				else if("application".equals(flow)) {
					securityScheme.getFlows()
						.clientCredentials(new OAuthFlow()
							.tokenUrl(tokenUrl)
							.scopes(scopes));
				}
				else if("accessCode".equals(flow)) {
					securityScheme.getFlows()
						.authorizationCode(new OAuthFlow()
							.authorizationUrl(authorizationUrl)
							.tokenUrl(tokenUrl)
							.scopes(scopes));
				}
			}
			securityScheme.setDescription((String)oas2SecurityScheme.get("description"));
		}

		return securityScheme;
	}

	private String convertRef(String ref) {
		if(ref == null) {
			return null;
		}

		if(ref.startsWith("#/definitions")) {
			return ref.replace("#/definitions", "#/components/schemas");
		}
		if(ref.startsWith("#/responses")) {
			return ref.replace("#/responses", "#/components/responses");
		}
		if(ref.startsWith("#/parameters")) {
			return ref.replace("#/parameters", "#/components/parameters");
		}
		if(ref.startsWith("#/securityDefinitions")) {
			return ref.replace("#/securityDefinitions", "#/components/securitySchemes");
		}
		// TODO: check another $ref
		return ref;
	}

}
