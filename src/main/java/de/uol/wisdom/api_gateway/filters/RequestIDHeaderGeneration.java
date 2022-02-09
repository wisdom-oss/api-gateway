package de.uol.wisdom.api_gateway.filters;

import com.sun.source.tree.BreakTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;

/**
 * A Filter which will assign a random request id to every request routed by the gateway
 */
@Component
public class RequestIDHeaderGeneration implements GlobalFilter {

	/**
	 * A Logger for this Filter
	 */
	final Logger logger = LoggerFactory.getLogger(RequestIDHeaderGeneration.class);

	/**
	 * The actual filter which is filtering the request
	 * @param exchange The {@link ServerWebExchange} which is currently handled
	 * @param chain A {@link GatewayFilterChain} which contains all filters which are still not
	 *                 executed
	 * @return The filtered exchange with the added request ID (X-Request-ID)
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// Generate a random uuid
		if (!isAlreadyRouted(exchange)) {

			// Access the headers
			HttpHeaders headers = exchange.getRequest().getHeaders();
			String requestId = headers.containsKey("X-Request-ID") ?
					                   headers.getFirst("X-Request-ID") :
					                   String.valueOf(UUID.randomUUID());
			logger.info("Generated/Using X-Request-ID('{}') for request from {}",
			            requestId, ZonedDateTime
					            .now(ZoneOffset.UTC)
					            .format(DateTimeFormatter.ISO_INSTANT)
			);
			// Get the request and add a header with a random uuid to it
			ServerHttpRequest request = exchange
					.getRequest()
					.mutate()
					.header("X-Request-ID", requestId)
					.build();
			return chain
					.filter(exchange.mutate().request(request).build())
					.then(Mono.fromRunnable(() -> exchange
							.getResponse()
							.getHeaders()
							.set("X-Request-ID", requestId)));
		}
		return chain.filter(exchange);
	}
}
