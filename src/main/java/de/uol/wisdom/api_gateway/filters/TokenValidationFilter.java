package de.uol.wisdom.api_gateway.filters;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;

/**
 * A Global Filter validating if the bearer token present is currently active and valid
 */
@Component
public class TokenValidationFilter implements GlobalFilter {

	/**
	 * Eureka Client used to check the availability at the service registry
	 */
	@Autowired
	private EurekaClient discoveryClient;

	/**
	 * Name of the authorization service used at the service registry
	 */
	private static final String AUTHORIZATION_SERVICE_NAME = "authorization-service";
	/**
	 * Path pointing to the token check endpoint
	 */
	private static final String CHECK_TOKEN_PATH = "/oauth/check_token";

	/**
	 * This UUID is used to test a valid token. This is only applicable if the
	 * Spring application is running in the "test" profile exclusively
	 */
	private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";

	/**
	 * Currently active SpringBoot Profiles, (defaults to "production")
	 */
	@Value("${spring.profiles.active:production}")
	private String activeProfiles;

	/**
	 * Logger for this application
	 */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	/**
	 * Check if the authorization service is registered at the registry and has the status UP
	 * @return True if the service is reachable
	 */
	private boolean authorizationServiceAvailable() {
		try {
			InstanceInfo authorizationServiceInfo = discoveryClient.getNextServerFromEureka(
					AUTHORIZATION_SERVICE_NAME, false
			);
			return authorizationServiceInfo.getStatus() == InstanceInfo.InstanceStatus.UP;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/**
	 * The filter method
	 *
	 * This method describes how an incoming exchange is handled. This method is
	 * verified via tests
	 * @param exchange The incoming and outgoing exchange between the server and
	 *                 the client
	 * @param chain Other filters running after this filter.
	 * @return A Mono indicating the result of the exchange
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (!isAlreadyRouted(exchange)) {
			// Save the request locally
			ServerHttpRequest request = exchange.getRequest();
			// Save the headers locally
			HttpHeaders headers = request.getHeaders();
			// Determine the request id
			String requestId = headers.getFirst("X-Request-ID");
			//Check if the authorization service is reachable
			if (!headers.containsKey("X-Testing-Pass-ModuleCheck")) {
				if (!authorizationServiceAvailable()) {
					logger.warn("No instance of the authorization server could be found.");
					HttpHeaders header = new HttpHeaders();
					header.set("WWW-Authenticate", "Bearer");
					throw new WebClientResponseException(
							HttpStatus.UNAUTHORIZED.value(),
							"unauthorized",
							header,
							null,
							null
					);
				}
			}
			// Get the route id to exempt the requests going to the auth service from the token
			// check or the uri of the route points to the authorization service
			Route routeAttributes = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
			String routeId = routeAttributes != null ? routeAttributes.getId() : "NaN";
			String routeScope = routeAttributes != null ?
			                    (String) routeAttributes.getMetadata().get("scope") : "";
			// Try and build a string
			PathPattern matcher = new PathPatternParser().parse("/auth/**");
			boolean pathMatchesAuthService =
					matcher.matches(request.getPath().pathWithinApplication());
			if (routeId.equals("authorization-service") || pathMatchesAuthService) {
				logger.info("Exempted request {} from authorization token check. The target is " +
						            "the authorization service", requestId);
				return chain.filter(exchange);
			}

			if (!validAuthorizationHeader(headers)) {
				HttpHeaders header = new HttpHeaders();
				header.set("WWW-Authenticate", "Bearer");
				throw new WebClientResponseException(
						HttpStatus.UNAUTHORIZED.value(),
						"unauthorized",
						header,
						null,
						null
				);
			}

			String userToken = getAuthorizationToken(headers);
			// Create a call to the authorization server with the scope needed for this route
			try {
				if (!tokenValidForService(userToken, routeScope)) {
					HttpHeaders header = new HttpHeaders();
					header.set("WWW-Authenticate", "Bearer");
					throw new WebClientResponseException(
							HttpStatus.UNAUTHORIZED.value(),
							"unauthorized",
							header,
							null,
							null
					);
				}
			} catch (URISyntaxException e) {
				throw new WebClientResponseException(
						HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"no_valid_uri",
						null,
						null,
						null
				);
			}
			return chain.filter(exchange);
		}
		return chain.filter(exchange);
	}

	/**
	 * Check if the testing mode is active (only the "test" profile is active)
	 * @return True if the test mode is active, false if it is not active
	 */
	private boolean isTestMode() {
		return activeProfiles.equals("test");
	}

	/**
	 * Check if an authorization token is valid for the scope of the route.
	 * @param userToken Authorization Token from the Headers
	 * @param routeScope Scope configured in the route's metadata
	 * @return True of the token is valid else false
	 */
	private boolean tokenValidForService(String userToken, String routeScope) throws URISyntaxException {
		if (isTestMode() && userToken.equals(NIL_UUID)) {
			return true;
		}
		if (isTestMode()) {
			return false;
		}
		// Build a http string pointing to one of the active authorization service containers
		InstanceInfo authorizationService = discoveryClient.getNextServerFromEureka(
				AUTHORIZATION_SERVICE_NAME, false
		);
		String authorizationServiceIP = authorizationService.getIPAddr();
		int authorizationServicePort = authorizationService.getPort();
		String authorizationServiceURL =
				"http://" + authorizationServiceIP + ":" + authorizationServicePort + CHECK_TOKEN_PATH;
		WebClient client = WebClient.create();
		Map<String, String> body = new HashMap<>();
		body.put("token", userToken);
		ResponseEntity<String> response = client
			.post()
			.uri(new URI(authorizationServiceURL))
			.header("Authorization", "Bearer " + userToken)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.accept(MediaType.APPLICATION_JSON)
			.body(BodyInserters.fromValue(body))
			.retrieve()
			.toEntity(String.class)
			.block();
		if (response == null) {
			return false;
		}
		if (!response.hasBody()) {
			return false;
		}
		JSONObject introspectionResult = new JSONObject(response.getBody());
		if (!introspectionResult.has("active")) {
			return false;
		}
		return introspectionResult.getBoolean("active");
	}

	/**
	 * Get the authorization token from the headers
	 * @param headers HttpHeaders
	 * @return An empty string if there is no value else the token
	 */
	private String getAuthorizationToken(HttpHeaders headers) {
		String authorizationHeaderValue = headers.getFirst("Authorization");
		if (authorizationHeaderValue != null)
			return authorizationHeaderValue.replaceAll("Bearer\s*", "");
		else
			return "";
	}

	/**
	 * Check if the headers contain a valid authorization header.
	 * @param headers Header of the request
	 * @return True if the Header is valid, false if it is not
	 */
	private boolean validAuthorizationHeader(HttpHeaders headers) {
		// Get the Authorization header
		String authorizationHeaderValue = headers.getFirst("Authorization");
		if (authorizationHeaderValue == null) {
			return false;
		}
		// Try to find the method "Bearer"
		boolean hasBearerAsAuthorizationMethod = authorizationHeaderValue.contains("Bearer");
		// Remove the Authorization Method from the header value
		String possibleToken = authorizationHeaderValue.replaceAll("Bearer\s*", "");
		try {
			//noinspection ResultOfMethodCallIgnored
			UUID.fromString(possibleToken);
		} catch (IllegalArgumentException e) {
			logger.warn("Illegal Token found. Denying access to the system");
			return false;
		}
		return hasBearerAsAuthorizationMethod;
	}
}
