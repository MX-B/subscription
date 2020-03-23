package io.gr1d.portal.subscriptions.config;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPlanValidator.class)
public @interface ValidPlan {

	String message() default "";
	Class<?>[] groups() default { };
	Class<? extends Payload>[] payload() default { };
}
