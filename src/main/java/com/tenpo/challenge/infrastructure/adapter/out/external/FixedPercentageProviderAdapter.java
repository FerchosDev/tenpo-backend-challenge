package com.tenpo.challenge.infrastructure.adapter.out.external;

import com.tenpo.challenge.domain.port.out.PercentageProviderPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Simula el servicio externo de porcentaje.
 */
@Component
public class FixedPercentageProviderAdapter implements PercentageProviderPort {

    private static final BigDecimal FIXED_PERCENTAGE = BigDecimal.valueOf(10);

    @Override
    public BigDecimal getPercentage() {
        return FIXED_PERCENTAGE;
    }

}
