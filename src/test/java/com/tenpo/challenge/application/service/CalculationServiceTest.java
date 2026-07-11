package com.tenpo.challenge.application.service;

import com.tenpo.challenge.domain.model.Calculation;
import com.tenpo.challenge.domain.port.out.PercentageProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock
    private PercentageProviderPort percentageProviderPort;

    private CalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new CalculationService(percentageProviderPort);
    }

    @Test
    void shouldAddPercentageToSum() {
        when(percentageProviderPort.getPercentage()).thenReturn(BigDecimal.valueOf(10));

        Calculation result = calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5));

        assertThat(result.num1()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(result.num2()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(result.result()).isEqualByComparingTo(BigDecimal.valueOf(11));
    }

    @Test
    void shouldReturnSumUnchangedWhenPercentageIsZero() {
        when(percentageProviderPort.getPercentage()).thenReturn(BigDecimal.ZERO);

        Calculation result = calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5));

        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.result()).isEqualByComparingTo(BigDecimal.TEN);
    }

}
