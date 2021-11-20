package de.uol.wisdom.api_gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RequestIDHeaderGeneration implements GlobalFilter {

	final Logger logger = LoggerFactory.getLogger(RequestIDHeaderGeneration.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// Generate a random uuid
		String requestId = String.valueOf(UUID.randomUUID());
		logger.info("Generated X-Request-ID('{}') for request from {}",
		            requestId, ZonedDateTime
				            .now(ZoneOffset.UTC)
				            .format(DateTimeFormatter.ISO_INSTANT)
		);
		// Get the request and add a header with a random uuid to it
		exchange
				.getRequest()
				.mutate()
				.header("X-Request-ID", String.valueOf(requestId));
		return chain
				.filter(exchange)
				.then(Mono.fromRunnable(() -> {
					exchange
							.getResponse()
							.getHeaders()
							.add("X-Request-ID", String.valueOf(requestId));
				}));

	}
}
