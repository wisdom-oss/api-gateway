package de.uol.wisdom.api_gateway;

import de.uol.wisdom.api_gateway.filters.AllowCORSForDevelopmentFilter;
import de.uol.wisdom.api_gateway.filters.RequestIDHeaderGeneration;
import de.uol.wisdom.api_gateway.filters.TokenValidationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	@Order(-2)
	public GlobalFilter corsBypass() {
		return new AllowCORSForDevelopmentFilter();
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
