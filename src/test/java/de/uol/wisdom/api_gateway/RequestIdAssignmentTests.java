package de.uol.wisdom.api_gateway;


import de.uol.wisdom.api_gateway.filters.RequestIDHeaderGeneration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions", "unused"})
@SpringBootTest(properties = "eureka.client.enabled=false", classes = {RequestIDHeaderGeneration.class})
public class RequestIdAssignmentTests {

	/**
	 * An instance of the TokenValidationFilter
	 */
	@Autowired
	private RequestIDHeaderGeneration requestIDHeaderGeneration;

	/**
	 * Gateway Filter Chain needed to filter the request and response. This will just return the
	 * exchange which was sent to it
	 */
	private final GatewayFilterChain filterChain = exchange -> Mono.fromRunnable(
			() -> exchange.getResponse().setStatusCode(HttpStatus.OK)
	);

	/**
	 * Logger for this test to log some extra information
	 */
	final Logger logger = LoggerFactory.getLogger(RequestIdAssignmentTests.class);

	/**
	 * Test if the http header "X-Request-ID" was created in the downstream and
	 * upstream GET request
	 */
	@Test
	void testRequestIDCreation_GET() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.get("/")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Filter the request
		requestIDHeaderGeneration
				.filter(mockedExchange, filterChain)
				.block();
		// Access the filtered request and response
		ServerHttpRequest filteredRequest = mockedExchange.getRequest();
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		// Access the headers of the request and response
		HttpHeaders requestHeaders = filteredRequest.getHeaders();
		HttpHeaders responseHeaders = filteredResponse.getHeaders();
		// Test the values
		assertNotNull(requestHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the downstream request");
		assertNotNull(responseHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the upstream response");
		// Test if the headers have the same value
		assertEquals(
				responseHeaders.getFirst("X-Request-ID"),
				requestHeaders.getFirst("X-Request-ID")
		);
		logger.info(
				"Found the same values in the headers. DOWNSTREAM: {} | UPSTREAM: {}",
	            requestHeaders.getFirst("X-Request-ID"),
	            responseHeaders.getFirst("X-Request-ID")
		);
		// Test if the headers are in the UUID format
		assertDoesNotThrow(() -> {
			UUID requestUUID = UUID.fromString(requestHeaders.getFirst("X-Request-ID"));
			UUID responseUUID = UUID.fromString(responseHeaders.getFirst("X-Request-ID"));
		});
		logger.info("The headers are in the expected UUID format");
		logger.info("TEST EXECUTED SUCCESSFULLY");
	}

	/**
	 * Test if the http header "X-Request-ID" was created in the downstream and
	 * upstream POST request
	 */
	@Test
	void testRequestIDCreation_POST() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.post("/")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Filter the request
		requestIDHeaderGeneration
				.filter(mockedExchange, filterChain)
				.block();
		// Access the filtered request and response
		ServerHttpRequest filteredRequest = mockedExchange.getRequest();
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		// Access the headers of the request and response
		HttpHeaders requestHeaders = filteredRequest.getHeaders();
		HttpHeaders responseHeaders = filteredResponse.getHeaders();
		// Test the values
		assertNotNull(requestHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the downstream request");
		assertNotNull(responseHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the upstream response");
		// Test if the headers have the same value
		assertEquals(
				responseHeaders.getFirst("X-Request-ID"),
				requestHeaders.getFirst("X-Request-ID")
		);
		logger.info(
				"Found the same values in the headers. DOWNSTREAM: {} | UPSTREAM: {}",
				requestHeaders.getFirst("X-Request-ID"),
				responseHeaders.getFirst("X-Request-ID")
		);
		// Test if the headers are in the UUID format
		assertDoesNotThrow(() -> {
			UUID requestUUID = UUID.fromString(requestHeaders.getFirst("X-Request-ID"));
			UUID responseUUID = UUID.fromString(responseHeaders.getFirst("X-Request-ID"));
		});
		logger.info("The headers are in the expected UUID format");
		logger.info("TEST EXECUTED SUCCESSFULLY");
	}

	/**
	 * Test if the http header "X-Request-ID" was created in the downstream and
	 * upstream PUT request
	 */
	@Test
	void testRequestIDCreation_PUT() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.put("/")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Filter the request
		requestIDHeaderGeneration
				.filter(mockedExchange, filterChain)
				.block();
		// Access the filtered request and response
		ServerHttpRequest filteredRequest = mockedExchange.getRequest();
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		// Access the headers of the request and response
		HttpHeaders requestHeaders = filteredRequest.getHeaders();
		HttpHeaders responseHeaders = filteredResponse.getHeaders();
		// Test the values
		assertNotNull(requestHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the downstream request");
		assertNotNull(responseHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the upstream response");
		// Test if the headers have the same value
		assertEquals(
				responseHeaders.getFirst("X-Request-ID"),
				requestHeaders.getFirst("X-Request-ID")
		);
		logger.info(
				"Found the same values in the headers. DOWNSTREAM: {} | UPSTREAM: {}",
				requestHeaders.getFirst("X-Request-ID"),
				responseHeaders.getFirst("X-Request-ID")
		);
		// Test if the headers are in the UUID format
		assertDoesNotThrow(() -> {
			UUID requestUUID = UUID.fromString(requestHeaders.getFirst("X-Request-ID"));
			UUID responseUUID = UUID.fromString(responseHeaders.getFirst("X-Request-ID"));
		});
		logger.info("The headers are in the expected UUID format");
		logger.info("TEST EXECUTED SUCCESSFULLY");
	}

	/**
	 * Test if the http header "X-Request-ID" was created in the downstream and
	 * upstream PATCH request
	 */
	@Test
	void testRequestIDCreation_PATCH() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.patch("/")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Filter the request
		requestIDHeaderGeneration
				.filter(mockedExchange, filterChain)
				.block();
		// Access the filtered request and response
		ServerHttpRequest filteredRequest = mockedExchange.getRequest();
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		// Access the headers of the request and response
		HttpHeaders requestHeaders = filteredRequest.getHeaders();
		HttpHeaders responseHeaders = filteredResponse.getHeaders();
		// Test the values
		assertNotNull(requestHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the downstream request");
		assertNotNull(responseHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the upstream response");
		// Test if the headers have the same value
		assertEquals(
				responseHeaders.getFirst("X-Request-ID"),
				requestHeaders.getFirst("X-Request-ID")
		);
		logger.info(
				"Found the same values in the headers. DOWNSTREAM: {} | UPSTREAM: {}",
				requestHeaders.getFirst("X-Request-ID"),
				responseHeaders.getFirst("X-Request-ID")
		);
		// Test if the headers are in the UUID format
		assertDoesNotThrow(() -> {
			UUID requestUUID = UUID.fromString(requestHeaders.getFirst("X-Request-ID"));
			UUID responseUUID = UUID.fromString(responseHeaders.getFirst("X-Request-ID"));
		});
		logger.info("The headers are in the expected UUID format");
		logger.info("TEST EXECUTED SUCCESSFULLY");
	}

	/**
	 * Test if the http header "X-Request-ID" was created in the downstream and
	 * upstream PATCH request
	 */
	@Test
	void testRequestIDCreation_DELETE() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.delete("/")
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Filter the request
		requestIDHeaderGeneration
				.filter(mockedExchange, filterChain)
				.block();
		// Access the filtered request and response
		ServerHttpRequest filteredRequest = mockedExchange.getRequest();
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		// Access the headers of the request and response
		HttpHeaders requestHeaders = filteredRequest.getHeaders();
		HttpHeaders responseHeaders = filteredResponse.getHeaders();
		// Test the values
		assertNotNull(requestHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the downstream request");
		assertNotNull(responseHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the upstream response");
		// Test if the headers have the same value
		assertEquals(
				responseHeaders.getFirst("X-Request-ID"),
				requestHeaders.getFirst("X-Request-ID")
		);
		logger.info(
				"Found the same values in the headers. DOWNSTREAM: {} | UPSTREAM: {}",
				requestHeaders.getFirst("X-Request-ID"),
				responseHeaders.getFirst("X-Request-ID")
		);
		// Test if the headers are in the UUID format
		assertDoesNotThrow(() -> {
			UUID requestUUID = UUID.fromString(requestHeaders.getFirst("X-Request-ID"));
			UUID responseUUID = UUID.fromString(responseHeaders.getFirst("X-Request-ID"));
		});
		logger.info("The headers are in the expected UUID format");
		logger.info("TEST EXECUTED SUCCESSFULLY");
	}

	/**
	 * Test if an already present value is overwritten
	 */
	@Test
	void testRequestIDCreation_HeaderAlreadyExists() {
		// Create a new mocked request
		MockServerHttpRequest mockedRequest = MockServerHttpRequest
				.get("/")
                 .header("X-Request-ID", String.valueOf(UUID.randomUUID()))
				.build();
		// Create a WebExchange with the mocked request
		MockServerWebExchange mockedExchange = MockServerWebExchange.from(mockedRequest);
		// Filter the request
		requestIDHeaderGeneration
				.filter(mockedExchange, filterChain)
				.block();
		// Access the filtered request and response
		ServerHttpRequest filteredRequest = mockedExchange.getRequest();
		ServerHttpResponse filteredResponse = mockedExchange.getResponse();
		// Access the headers of the request and response
		HttpHeaders requestHeaders = filteredRequest.getHeaders();
		HttpHeaders responseHeaders = filteredResponse.getHeaders();
		// Test the values
		assertNotNull(requestHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the downstream request");
		assertNotNull(responseHeaders.getFirst("X-Request-ID"));
		logger.info("Found request id header in the upstream response");
		// Test if the headers have the same value
		assertEquals(
				responseHeaders.getFirst("X-Request-ID"),
				requestHeaders.getFirst("X-Request-ID")
		);
		logger.info(
				"Found the same values in the headers. DOWNSTREAM: {} | UPSTREAM: {}",
				requestHeaders.getFirst("X-Request-ID"),
				responseHeaders.getFirst("X-Request-ID")
		);
		// Test if the headers are in the UUID format
		assertDoesNotThrow(() -> {
			UUID.fromString(requestHeaders.getFirst("X-Request-ID"));
			UUID.fromString(responseHeaders.getFirst("X-Request-ID"));
		});
		logger.info("The headers are in the expected UUID format");
		logger.info("TEST EXECUTED SUCCESSFULLY");
	}
}
