package org.zerofour.swagger2.converter.advice;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.zerofour.swagger2.converter.oas3.JSON;
import org.zerofour.swagger2.converter.oas3.OAS3Converter;
import springfox.documentation.spring.web.json.Json;
import springfox.documentation.swagger2.web.Swagger2Controller;

import java.io.IOException;
import java.lang.reflect.Method;

@ControllerAdvice
public class OAS3ConverterAdvice implements ResponseBodyAdvice<Json> {

	private final Class<?> SWAGGER_CONTROLLER_CLASS = Swagger2Controller.class;
	private final String SWAGGER_CONTROLLER_METHOD = "getDocumentation";

	private OAS3Converter oas3Converter = new OAS3Converter();
	private Json cachedBody = null;

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		Method method = returnType.getMethod();
		if (method != null
			&& method.getDeclaringClass() == SWAGGER_CONTROLLER_CLASS
			&& method.getName().equals(SWAGGER_CONTROLLER_METHOD)) {
			return true;
		}
		return false;
	}

	@Override
	public Json beforeBodyWrite(Json body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

		if(cachedBody != null) {
			return cachedBody;
		}

		try {
			JSON oas2 = JSON.readValue(body.value());
			System.out.println(oas2);
			OpenAPI openAPI = oas3Converter.convertOpenAPI(oas2);
			if(openAPI != null) {
				body = new Json(JSON.readValue(openAPI).writeValue());
				cachedBody = body;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return body;
	}

}
