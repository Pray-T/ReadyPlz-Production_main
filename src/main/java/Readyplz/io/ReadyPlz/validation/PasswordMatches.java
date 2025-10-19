package Readyplz.io.ReadyPlz.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = Readyplz.io.ReadyPlz.validation.PasswordMatchesValidator.class)
@Documented
public @interface PasswordMatches {
    String message() default "password_do_not_match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String newPasswordField() default "newPassword";
    String confirmPasswordField() default "confirmPassword";
}


