package org.zerofour.swagger2.converter.annotation;

import org.springframework.context.annotation.Import;
import org.zerofour.swagger2.converter.configuration.OAS3Configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = { java.lang.annotation.ElementType.TYPE })
@Documented
@Import({OAS3Configuration.class})
public @interface EnableSwagger2OAS3 {
}
