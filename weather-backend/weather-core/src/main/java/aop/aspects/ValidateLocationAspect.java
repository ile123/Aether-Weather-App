package aop.aspects;

import aop.ValidateLocation;
import exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

@Aspect
@Component
@Slf4j
public class ValidateLocationAspect {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    @Around("@annotation(validateLocation)")
    public Object validateLocation(ProceedingJoinPoint joinPoint,
                                   ValidateLocation validateLocation) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(validateLocation.paramName())) {
                String location = (String) args[i];
                validateLocationValue(location);
                break;
            }
        }

        return joinPoint.proceed();
    }

    private void validateLocationValue(String location) {
        if (location == null || location.isBlank()) {
            throw new ValidationException(
                    "location", "Location must not be blank");
        }
        if (location.length() < MIN_LENGTH) {
            throw new ValidationException(
                    "location", "Location must be at least " + MIN_LENGTH + " characters");
        }
        if (location.length() > MAX_LENGTH) {
            throw new ValidationException(
                    "location", "Location must not exceed " + MAX_LENGTH + " characters");
        }
        if (!location.matches("^[a-zA-Z0-9\\s,.-]+$")) {
            throw new ValidationException(
                    "location", "Location contains invalid characters");
        }
    }
}