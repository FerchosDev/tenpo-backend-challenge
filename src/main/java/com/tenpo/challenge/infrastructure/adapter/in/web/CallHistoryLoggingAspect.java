package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.domain.model.CallHistory;
import com.tenpo.challenge.domain.port.out.CallHistoryPort;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
public class CallHistoryLoggingAspect {

    private static final String CALCULATE_ENDPOINT = "/api/v1/calculate";

    private final CallHistoryPort callHistoryPort;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.tenpo.challenge.infrastructure.adapter.in.web.CalculationController.calculate(..))")
    public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String params = toJson(joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : null);

        try {
            Object response = joinPoint.proceed();
            callHistoryPort.save(new CallHistory(null, Instant.now(), CALCULATE_ENDPOINT, params, toJson(response), null, 200));
            return response;
        } catch (Exception ex) {
            callHistoryPort.save(new CallHistory(null, Instant.now(), CALCULATE_ENDPOINT, params, null, ex.getMessage(), 500));
            throw ex;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

}
