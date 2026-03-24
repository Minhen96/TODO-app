package com.taskplatform.gateway.filter;

import com.taskplatform.gateway.config.AuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient webClient;
    private final AuthConfig authConfig;

    public AuthenticationFilter(WebClient.Builder webClientBuilder, AuthConfig authConfig) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
        this.authConfig = authConfig;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("Missing or invalid Authorization header");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory()
                                .wrap("{\"error\":\"Missing or invalid Authorization header\"}".getBytes()))
                );
            }

            String token = authHeader.substring(7);

            return validateToken(token)
                    .flatMap(validationResponse -> {
                        if (validationResponse.valid()) {
                            // Add user info to headers for downstream services
                            var mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id", validationResponse.userId())
                                    .header("X-User-Name", validationResponse.username())
                                    .header("X-User-Roles", String.join(",", validationResponse.roles()))
                                    .build();

                            log.debug("Request authenticated",
                                    kv("userId", validationResponse.userId()),
                                    kv("path", exchange.getRequest().getPath()));

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        } else {
                            log.warn("Token validation failed",
                                    kv("error", validationResponse.error()),
                                    kv("path", exchange.getRequest().getPath()));

                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory()
                                            .wrap(("{\"error\":\"" + validationResponse.error() + "\"}").getBytes()))
                            );
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("Error validating token", e);
                        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        return exchange.getResponse().writeWith(
                                Mono.just(exchange.getResponse().bufferFactory()
                                        .wrap("{\"error\":\"Authentication service unavailable\"}".getBytes()))
                        );
                    });
        };
    }

    private Mono<TokenValidationResponse> validateToken(String token) {
        return webClient.get()
                .uri(authConfig.getValidateUrl() + "?token=" + token)
                .retrieve()
                .bodyToMono(TokenValidationResponse.class);
    }

    public static class Config {
        // Configuration properties can be added here
    }

    private record TokenValidationResponse(
            boolean valid,
            String userId,
            String username,
            java.util.List<String> roles,
            String error
    ) {}
}
