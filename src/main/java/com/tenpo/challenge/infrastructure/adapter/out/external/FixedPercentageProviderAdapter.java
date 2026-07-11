package com.tenpo.challenge.infrastructure.adapter.out.external;

import com.tenpo.challenge.domain.exception.ExternalServiceException;
import com.tenpo.challenge.domain.port.out.PercentageProviderPort;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Simula el servicio externo de porcentaje.
 */
@Component
public class FixedPercentageProviderAdapter implements PercentageProviderPort {

    private static final BigDecimal FIXED_PERCENTAGE = BigDecimal.valueOf(10);

    @Override
    @Retry(name = "percentageProvider", fallbackMethod = "fallbackPercentage")
    public BigDecimal getPercentage() {
        return FIXED_PERCENTAGE;
    }

    private BigDecimal fallbackPercentage(Throwable throwable) {
        throw new ExternalServiceException("Percentage provider failed after 3 retries", throwable);
    }

}
