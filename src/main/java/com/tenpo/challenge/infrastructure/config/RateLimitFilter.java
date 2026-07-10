package com.tenpo.challenge.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.domain.model.CallHistory;
import com.tenpo.challenge.domain.port.out.CallHistoryPort;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Limita globalmente los endpoints /api/** a 3 requests por minuto,
 * compartido entre todos los clientes (no es un límite por IP/usuario).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 3;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    private static final String HISTORY_ENDPOINT = "/api/v1/history";

    private final Bucket bucket;
    private final ObjectMapper objectMapper;
    private final CallHistoryPort callHistoryPort;

    public RateLimitFilter(ObjectMapper objectMapper, CallHistoryPort callHistoryPort) {
        this.objectMapper = objectMapper;
        this.callHistoryPort = callHistoryPort;
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillIntervally(CAPACITY, REFILL_PERIOD)
                .build();
        this.bucket = Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        String message = "Rate limit exceeded: maximum " + CAPACITY + " requests per minute allowed.";
        RateLimitExceededResponse body = new RateLimitExceededResponse(message, Instant.now(), retryAfterSeconds);

        // Se loguea acá, no en CallHistoryLoggingAspect: el request nunca
        // llega a despachar al controller, así que ese @Around nunca se
        // ejecuta para las llamadas rechazadas por rate limit. No se
        // capturan params: leer el body acá lo consumiría antes de que
        // llegue (si acaso llegara) al controller. Se excluye el propio
        // endpoint de historial: no tiene sentido que una consulta al
        // historial quede registrada dentro del historial.
        if (!HISTORY_ENDPOINT.equals(request.getRequestURI())) {
            callHistoryPort.save(new CallHistory(
                    null, Instant.now(), request.getRequestURI(), null, null, message, HttpStatus.TOO_MANY_REQUESTS.value()));
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

}
