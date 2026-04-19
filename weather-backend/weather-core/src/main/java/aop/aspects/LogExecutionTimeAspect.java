package aop.aspects;

import aop.LogExecutionTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
@Slf4j
public class LogExecutionTimeAspect {

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint,
                                   LogExecutionTime logExecutionTime) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName()
                + "." + signature.getName();
        String label = logExecutionTime.value().isEmpty()
                ? methodName
                : logExecutionTime.value();

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return mono.doOnSuccess(v ->
                            log.info("[{}] completed in {} ms", label,
                                    System.currentTimeMillis() - start))
                    .doOnError(e ->
                            log.warn("[{}] failed after {} ms — {}",
                                    label, System.currentTimeMillis() - start, e.getMessage()));
        }

        if (result instanceof Flux<?> flux) {
            return flux.doOnComplete(() ->
                            log.info("[{}] completed in {} ms", label,
                                    System.currentTimeMillis() - start))
                    .doOnError(e ->
                            log.warn("[{}] failed after {} ms — {}",
                                    label, System.currentTimeMillis() - start, e.getMessage()));
        }

        log.info("[{}] completed in {} ms", label, System.currentTimeMillis() - start);
        return result;
    }
}