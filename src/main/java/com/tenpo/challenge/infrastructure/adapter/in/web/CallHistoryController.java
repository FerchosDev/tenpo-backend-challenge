package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.exception.InvalidPaginationException;
import com.tenpo.challenge.infrastructure.adapter.out.persistence.CallHistoryReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Call History", description = "Historial de invocaciones a la API, persistido de forma asíncrona")
public class CallHistoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CallHistoryReader callHistoryReader;

    @Operation(
            summary = "Obtiene el historial de llamadas paginado",
            description = "Devuelve el historial de invocaciones a la API ordenado por fecha/hora descendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de historial obtenida con éxito",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos (page negativo, "
                    + "size fuera de rango, o de un tipo no numérico)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Se superó el límite de 3 requests por minuto",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history")
    public ResponseEntity<PagedResponse<CallHistoryResponse>> getHistory(
            @Parameter(description = "Número de página (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de elementos por página (máximo " + MAX_PAGE_SIZE + ")", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<CallHistoryResponse> resultPage = callHistoryReader.findAll(pageable).map(CallHistoryResponse::from);
        return ResponseEntity.ok(PagedResponse.from(resultPage));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException("page must be greater than or equal to 0, but was " + page);
        }
        if (size <= 0) {
            throw new InvalidPaginationException("size must be greater than 0, but was " + size);
        }
        if (size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException("size must not exceed " + MAX_PAGE_SIZE + ", but was " + size);
        }
    }

}
