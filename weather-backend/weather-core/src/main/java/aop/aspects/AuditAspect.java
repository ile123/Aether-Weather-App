package aop.aspects;

import aop.Audited;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint,
                        Audited audited) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName()
                + "." + signature.getName();
        String action = audited.action().isEmpty()
                ? methodName
                : audited.action();

        if (audited.logArgs()) {
            log.info("[AUDIT] action={} args={}", action,
                    Arrays.toString(joinPoint.getArgs()));
        } else {
            log.info("[AUDIT] action={}", action);
        }

        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return mono
                    .doOnSuccess(v -> log.info("[AUDIT] action={} status=SUCCESS", action))
                    .doOnError(e -> log.warn("[AUDIT] action={} status=FAILED error={}",
                            action, e.getMessage()));
        }

        if (result instanceof Flux<?> flux) {
            return flux
                    .doOnComplete(() -> log.info("[AUDIT] action={} status=SUCCESS", action))
                    .doOnError(e -> log.warn("[AUDIT] action={} status=FAILED error={}",
                            action, e.getMessage()));
        }

        log.info("[AUDIT] action={} status=SUCCESS", action);
        return result;
    }
}