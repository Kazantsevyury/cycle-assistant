package com.example.menstrualcyclebot.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Pointcut для методов в вашем боте
    @Pointcut("execution(* com.example.menstrualcyclebot.presentation.Bot.*(..))")
    public void botMethods() {}

    // Логирование входных параметров, времени выполнения и результата
    @Around("botMethods()")
    public Object logExecutionDetails(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Логируем вызов метода и его параметры
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info("Метод {} вызван с параметрами: {}", methodName, args);

        Object result;
        try {
            // Выполняем метод и получаем результат
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Логируем успешное завершение метода и результат
            log.info("Метод {} завершен за {} мс с результатом: {}", methodName, duration, result);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Логируем исключение, если оно возникло
            log.error("Ошибка в методе {} спустя {} мс. Ошибка: {}", methodName, duration, e.getMessage(), e);
            throw e;
        }

        return result;
    }
}