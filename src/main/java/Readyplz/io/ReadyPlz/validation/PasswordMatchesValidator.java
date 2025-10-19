package Readyplz.io.ReadyPlz.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import java.util.Objects;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String newPasswordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.newPasswordField = constraintAnnotation.newPasswordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(value);
        Object newVal = wrapper.getPropertyValue(newPasswordField);
        Object confirmVal = wrapper.getPropertyValue(confirmPasswordField);
        return Objects.equals(newVal, confirmVal);      //newVal과 confirmVal이 **모두 null**일 때 -> true (두 필드가 비어있는 경우는 유효), 
                                                                                 //둘 중 **하나만 null**일 때 -> false (값이 다르므로 유효하지 않음),                              
                                                                                 //둘 다 null이 아닐 때 -> 내부적으로 newVal.equals(confirmVal)를 호출하여 실제 값을 비교
    }
}


