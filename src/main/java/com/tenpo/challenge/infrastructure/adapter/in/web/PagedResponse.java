package com.tenpo.challenge.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        @Schema(description = "Contenido de la página actual") List<T> content,
        @Schema(description = "Número de página actual (0-indexed)", example = "0") int page,
        @Schema(description = "Tamaño de página", example = "20") int size,
        @Schema(description = "Cantidad total de elementos en todas las páginas", example = "42") long totalElements,
        @Schema(description = "Cantidad total de páginas", example = "3") int totalPages,
        @Schema(description = "Indica si esta es la última página", example = "false") boolean last
) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

}
