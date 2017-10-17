package net.unit8.bouncr.web.validator;

import net.unit8.bouncr.web.constraints.StringEnumeration;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static enkan.util.ReflectionUtils.tryReflection;

public class StringEnumerationValidator implements ConstraintValidator<StringEnumeration, String> {
    private Set<String> availableEnumNames;
    @Override
    public void initialize(StringEnumeration stringEnumeration) {
        Class<? extends Enum<?>> enumClass = stringEnumeration.enumClass();
        String accessorMethodName = stringEnumeration.accessorMethod();
        Method accessorMethod = tryReflection(() -> enumClass.getDeclaredMethod(accessorMethodName));
        if (!accessorMethod.getReturnType().equals(String.class)) {
            throw new IllegalArgumentException("accessorMethod must return String");
        }
        availableEnumNames = Arrays.stream(enumClass.getEnumConstants())
                .map(e -> tryReflection(() -> (String) accessorMethod.invoke(e)))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else {
            return availableEnumNames.contains(value);
        }
    }
}
