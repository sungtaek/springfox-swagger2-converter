package org.zerofour.swagger2.converter.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zerofour.swagger2.converter.advice.OAS3ConverterAdvice;

@Configuration
public class OAS3Configuration {
	@Bean
	public OAS3ConverterAdvice oas3ConverterAdvice() {
		return new OAS3ConverterAdvice();
	}
}
