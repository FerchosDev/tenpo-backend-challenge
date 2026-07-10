package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.model.Calculation;
import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import com.tenpo.challenge.infrastructure.config.RateLimitExceededResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Calculation", description = "Suma de dos números con porcentaje externo dinámico")
public class CalculationController {

    private final CalculationUseCase calculationUseCase;

    @Operation(
            summary = "Calcula num1 + num2 aplicando el porcentaje del servicio externo",
            description = "Suma num1 y num2, obtiene un porcentaje de un servicio externo (mockeado, con "
                    + "reintentos automáticos ante fallos) y lo aplica sobre la suma."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cálculo realizado con éxito",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CalculationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Error de validación en el request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Servicio externo de porcentaje no disponible tras agotar reintentos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Se superó el límite de 3 requests por minuto",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RateLimitExceededResponse.class)))
    })
    @PostMapping("/calculate")
    public CalculationResponse calculate(@Valid @RequestBody CalculationRequest request) {
        Calculation calculation = calculationUseCase.calculate(request.num1(), request.num2());
        return CalculationResponse.from(calculation);
    }

}
