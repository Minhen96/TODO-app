package com.taskplatform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate request ID if not present
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // Add request ID to response headers
        final String finalRequestId = requestId;
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Store start time
        exchange.getAttributes().put(START_TIME_ATTR, Instant.now());

        // Mutate request to include request ID
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        log.info("Incoming request",
                kv("requestId", requestId),
                kv("method", request.getMethod()),
                kv("path", request.getPath()),
                kv("clientIp", request.getRemoteAddress()));

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    Instant startTime = exchange.getAttribute(START_TIME_ATTR);
                    long duration = startTime != null ?
                            Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;

                    log.info("Request completed",
                            kv("requestId", finalRequestId),
                            kv("status", exchange.getResponse().getStatusCode()),
                            kv("durationMs", duration));
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
