package com.tenpo.challenge.application.service;

import com.tenpo.challenge.domain.model.Calculation;
import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import com.tenpo.challenge.domain.port.out.PercentageProviderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CalculationService implements CalculationUseCase {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int RESULT_SCALE = 2;

    private final PercentageProviderPort percentageProviderPort;

    @Override
    public Calculation calculate(BigDecimal num1, BigDecimal num2) {
        BigDecimal sum = num1.add(num2);
        BigDecimal percentage = percentageProviderPort.getPercentage();
        BigDecimal percentageAmount = sum.multiply(percentage)
                .divide(ONE_HUNDRED, RESULT_SCALE, RoundingMode.HALF_UP);
        BigDecimal result = sum.add(percentageAmount).setScale(RESULT_SCALE, RoundingMode.HALF_UP);

        return new Calculation(num1, num2, percentage, result);
    }

}
