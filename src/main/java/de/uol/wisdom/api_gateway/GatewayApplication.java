package de.uol.wisdom.api_gateway;

import de.uol.wisdom.api_gateway.filters.RequestIDHeaderGeneration;
import de.uol.wisdom.api_gateway.filters.TokenValidationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * The Spring Cloud based Service Gateway application
 */
@SpringBootApplication
public class GatewayApplication {

	/**
	 * The Startup Method for the Gateway
	 * @param args Additional arguments which will be passed to the Spring Application runner
	 */
	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}


	/**
	 * Activation of the {@link RequestIDHeaderGeneration Request ID Generator}
	 * @return A new instance of the filter
	 */
	@Bean
	@Order(-1)
	public GlobalFilter requestIdHeaderGenerator() {
		return new RequestIDHeaderGeneration();
	}

	/**
	 * Activation of the {@link TokenValidationFilter}
	 * @return A new instance of the {@link TokenValidationFilter}
	 */
	@Bean
	@Order(0)
	public GlobalFilter AuthorizationCheck() {
		return new TokenValidationFilter();
	}


}
