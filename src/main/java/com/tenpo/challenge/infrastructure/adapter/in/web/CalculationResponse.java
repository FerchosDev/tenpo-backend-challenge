package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.model.Calculation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CalculationResponse(
        @Schema(description = "First number provided in the request", example = "5") BigDecimal num1,
        @Schema(description = "Second number provided in the request", example = "5") BigDecimal num2,
        @Schema(description = "Percentage applied, obtained from the external percentage service", example = "10") BigDecimal percentage,
        @Schema(description = "Final result: (num1 + num2) plus the applied percentage", example = "11.00") BigDecimal result
) {

    public static CalculationResponse from(Calculation calculation) {
        return new CalculationResponse(
                calculation.num1(),
                calculation.num2(),
                calculation.percentage(),
                calculation.result()
        );
    }

}
