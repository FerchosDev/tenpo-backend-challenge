package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.model.Calculation;
import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CalculationController {

    private final CalculationUseCase calculationUseCase;

    @PostMapping("/calculate")
    public CalculationResponse calculate(@Valid @RequestBody CalculationRequest request) {
        Calculation calculation = calculationUseCase.calculate(request.num1(), request.num2());
        return CalculationResponse.from(calculation);
    }

}
