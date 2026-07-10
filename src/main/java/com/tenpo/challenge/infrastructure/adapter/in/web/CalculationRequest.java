package com.tenpo.challenge.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CalculationRequest(
        @Schema(description = "First number to add", example = "5")
        @NotNull(message = "num1 is required") BigDecimal num1,

        @Schema(description = "Second number to add", example = "5")
        @NotNull(message = "num2 is required") BigDecimal num2
) {
}
