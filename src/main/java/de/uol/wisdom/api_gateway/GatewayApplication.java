package de.uol.wisdom.api_gateway;

import de.uol.wisdom.api_gateway.filters.RequestIDHeaderGeneration;
import de.uol.wisdom.api_gateway.filters.TokenValidationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	@Order(-1)
	public GlobalFilter requestIdHeaderGenerator() {
		return new RequestIDHeaderGeneration();
	}

	@Bean
	@Order(0)
	public GlobalFilter AuthorizationCheck() {
		return new TokenValidationFilter();
	}

}
