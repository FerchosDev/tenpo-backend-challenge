package com.tenpo.challenge.infrastructure.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class CallHistoryLoggingAspect {

    private static final String CALCULATE_ENDPOINT = "/api/v1/calculate";

    private final CallHistoryRecorder callHistoryRecorder;

    @Around("execution(* com.tenpo.challenge.infrastructure.adapter.in.web.CalculationController.calculate(..))")
    public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
        Object params = joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : null;

        try {
            Object response = joinPoint.proceed();
            callHistoryRecorder.record(CALCULATE_ENDPOINT, params, response, null, 200);
            return response;
        } catch (Exception ex) {
            int status = ApiExceptionStatusResolver.resolve(ex).value();
            callHistoryRecorder.record(CALCULATE_ENDPOINT, params, null, ex.getMessage(), status);
            throw ex;
        }
    }

}
