package de.uol.wisdom.api_gateway;

import de.uol.wisdom.api_gateway.filters.TokenValidationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"spring.profiles.active=test"})
public class TokenValidationTests {

	/**
	 * NIL UUID for simulating invalid requests
	 */
	private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";

	/**
	 * Automatically created instance of the filter which will be tested
	 */
	@Autowired
	private TokenValidationFilter tokenValidationFilter;

	/**
	 * Gateway filter chain which will just return the exchange
	 */
	private final GatewayFilterChain filterChain = exchange -> Mono.fromRunnable(
			() -> exchange.getResponse().setStatusCode(HttpStatus.OK)
	);

	/**
	 * Test if the request will be denied with a 503 Response if the authorization server is not
	 * reachable or registered as up at the service registry
	 */
	@Test
	void RejectAuthorizationModuleOutageWith503() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.get("/")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Pass the exchange through the filter
		tokenValidationFilter
				.filter(mockedExchange, filterChain)
				.block();
		// Access the response object
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, filteredResponse.getStatusCode());
	}

	/**
	 * Test if the request will be returned with a 401 Header if no Authorization Header was set
	 */
	@Test
	void RejectMissingHeaderWith401() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.get("/")
				// This header is added to pass the check for the availability of the
				// authorization service
                .header("X-Testing-Pass-ModuleCheck", "true")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Pass the exchange through the filter
		tokenValidationFilter
				.filter(mockedExchange, filterChain)
				.block();
		// Access the response object
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		assertEquals(HttpStatus.UNAUTHORIZED, filteredResponse.getStatusCode());
	}

	/**
	 * Test if a token is rejected with a 403 when having a non-valid token present in the header
	 */
	@Test
	void RejectInvalidTokenWith403() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.get("/")
				// This header is added to pass the check for the availability of the
				// authorization service
				.header("X-Testing-Pass-ModuleCheck", "true")
				.header("Authorization", "Bearer " + UUID.randomUUID())
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Pass the exchange through the filter
		tokenValidationFilter
				.filter(mockedExchange, filterChain)
				.block();
		// Access the response object
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		assertEquals(HttpStatus.FORBIDDEN, filteredResponse.getStatusCode());
	}

	/**
	 * Test if a token is rejected with a 403 when having a non-valid token present in the header
	 *
	 */
	@Test
	void ContinueWithValidToken200() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.get("/")
				// This header is added to pass the check for the availability of the
				// authorization service
				.header("X-Testing-Pass-ModuleCheck", "true")
				.header("Authorization", "Bearer " + NIL_UUID)
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Pass the exchange through the filter
		tokenValidationFilter
				.filter(mockedExchange, filterChain)
				.block();
		// Access the response object
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		assertEquals(HttpStatus.OK, filteredResponse.getStatusCode());
	}

	@Test
	void DoNotCheckTokenInAuthorizationModuleRequests() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.get("/auth/")
				// This header is added to pass the check for the availability of the
				// authorization service
				.header("X-Testing-Pass-ModuleCheck", "true")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Pass the exchange through the filter
		tokenValidationFilter
				.filter(mockedExchange, filterChain)
				.block();
		// Access the response object
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		assertEquals(HttpStatus.OK, filteredResponse.getStatusCode());
	}

}
