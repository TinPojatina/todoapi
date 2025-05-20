package todo.kanban.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Documented
@Constraint(validatedBy = EnumValidator.Validator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValidator {
  String message() default "Invalid value. Allowed values are: {allowedValues}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  Class<? extends Enum<?>> enumClass();

  class Validator implements ConstraintValidator<EnumValidator, Object> {
    private List<String> allowedValues;

    @Override
    public void initialize(EnumValidator constraintAnnotation) {
      allowedValues =
          Stream.of(constraintAnnotation.enumClass().getEnumConstants())
              .map(Enum::name)
              .collect(Collectors.toList());
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
      if (value == null) {
        return true;
      }

      String stringValue = value.toString();
      boolean isValid = allowedValues.contains(stringValue);

      if (!isValid) {
        String message =
            context
                .getDefaultConstraintMessageTemplate()
                .replace("{allowedValues}", String.join(", ", allowedValues));

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
      }

      return isValid;
    }
  }
}
