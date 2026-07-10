package com.tenpo.challenge.domain.model;

import java.math.BigDecimal;

public record Calculation(
        BigDecimal num1,
        BigDecimal num2,
        BigDecimal percentage,
        BigDecimal result
) {
}
