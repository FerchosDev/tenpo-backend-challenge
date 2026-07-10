package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.infrastructure.adapter.out.persistence.CallHistoryReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Call History", description = "Historial de invocaciones a la API, persistido de forma asíncrona")
public class CallHistoryController {

    private final CallHistoryReader callHistoryReader;

    @Operation(
            summary = "Obtiene el historial de llamadas paginado",
            description = "Devuelve el historial de invocaciones a la API ordenado por fecha/hora descendente. "
                    + "Acepta los parámetros estándar de Spring Data: page y size."
    )
    @ApiResponse(responseCode = "200", description = "Página de historial obtenida con éxito",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping("/history")
    public ResponseEntity<PagedResponse<CallHistoryResponse>> getHistory(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        Pageable sortedByTimestampDesc = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );
        Page<CallHistoryResponse> page = callHistoryReader.findAll(sortedByTimestampDesc)
                .map(CallHistoryResponse::from);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

}
