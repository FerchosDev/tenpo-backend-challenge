package com.tenpo.challenge.domain.port.in;

import com.tenpo.challenge.domain.model.Calculation;

import java.math.BigDecimal;

public interface CalculationUseCase {

    Calculation calculate(BigDecimal num1, BigDecimal num2);

}
