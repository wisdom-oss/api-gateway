package de.uol.wisdom.api_gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;

public class AllowCORSForDevelopmentFilter implements GlobalFilter {

	/**
	 * The token used to enable bypassing CORS restrictions
	 */
	@Value("${CORS_BYPASS_TOKEN}")
	private String corsBypassToken;

	/**
	 * The methods allowed during CORS
	 */
	public ArrayList<HttpMethod> CORSMethods = new ArrayList<>(Arrays.asList(HttpMethod.values()));


	/**
	 * Logger for this filter
	 */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		logger.info("TESTING FOR CORS BYPASS");
		// Check if the bypass token was set
		if (corsBypassToken == null) {
			return chain.filter(exchange);
		}
		// Get the request present in the exchange
		ServerHttpRequest request = exchange.getRequest();
		// Access the headers of the request
		HttpHeaders headers = request.getHeaders();
		// Check if the X-CORS-BYPASS header is set
		String corsBypassHeaderValue = headers.getFirst("X-CORS-BYPASS");
		// Check if the header value is set
		if (corsBypassHeaderValue == null) {
			return chain.filter(exchange);
		} else if (corsBypassHeaderValue.equals(corsBypassToken)) {
			return chain
					.filter(exchange)
					       .then(Mono.fromRunnable(() -> {
							   ServerHttpResponse response = exchange.getResponse();
							   HttpHeaders responseHeaders = response.getHeaders();
							   // Set the Access Control Headers
						       responseHeaders.setAccessControlAllowMethods(CORSMethods);
							   responseHeaders.setAccessControlAllowOrigin("*");
							   responseHeaders.setAccessControlAllowCredentials(true);
							   exchange.mutate().response(response).build();

					       }));
		} else {
			return chain.filter(exchange);
		}

	}
}
