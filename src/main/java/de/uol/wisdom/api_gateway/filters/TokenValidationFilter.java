package de.uol.wisdom.api_gateway.filters;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;

@Component
public class TokenValidationFilter implements GlobalFilter {

	@Autowired
	private EurekaClient discoveryClient;

	private static final String AUTHORIZATION_SERVICE_NAME = "authorization-service";
	private static final String CHECK_TOKEN_PATH = "/oauth/check_token";

	Logger logger = LoggerFactory.getLogger(TokenValidationFilter.class);


	/**
	 * Check if the authorization service is registered at the registry and has the status UP
	 * @return True if the service is reachable
	 */
	private boolean authorizationServiceAvailable() {
		try {
			InstanceInfo authorizationServiceInfo = discoveryClient.getNextServerFromEureka(
					AUTHORIZATION_SERVICE_NAME, false
			);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (!isAlreadyRouted(exchange)) {
			logger.info("EXCHANGE IS NOT ROUTED");
			// Save the headers locally
			HttpHeaders headers = exchange.getRequest().getHeaders();
			// Determine the request id
			String requestId = headers.getFirst("X-Request-ID");
			// Get the route id to exempt the requests going to the auth service from the token
			// check or the uri of the route points to the authoritzation service
			Route routeAttributes = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
			String routeId = routeAttributes != null ? routeAttributes.getId() : "NaN";
			String routeScope = routeAttributes != null ?
			                    (String) routeAttributes.getMetadata().get("scope") : "";
			logger.info("DETERMINED ROUTEID: {}", routeId);
			if (routeId.equals("authorization-service")) {
				logger.info("Exempted request {} from authorization token check. The target is " +
						            "the authorization service", requestId);
				return chain.filter(exchange);
			}
			if (!authorizationServiceAvailable()) {
				return Mono.fromRunnable(() -> {
					ServerHttpResponse response = exchange.getResponse();
					response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
					exchange
							.mutate()
							.response(response);
				});
			}
			if (!validAuthorizationHeader(headers)) {
				return Mono.fromRunnable(() -> {
					ServerHttpResponse response = exchange.getResponse();
					response.setStatusCode(HttpStatus.UNAUTHORIZED);
					exchange
							.mutate()
							.response(response);
				});
			}
			String userToken = getAuthorizationToken(headers);
			// Create a call to the authorization server with the scope needed for this route
			if (!tokenValidForService(userToken, routeScope)) {
				return Mono.fromRunnable(() -> {
					ServerHttpResponse response = exchange.getResponse();
					response.setStatusCode(HttpStatus.FORBIDDEN);
					exchange
							.mutate()
							.response(response);
				});
			}
			return chain.filter(exchange);
		}
		return chain.filter(exchange);
	}

	private boolean tokenValidForService(String userToken, String routeScope) {
		// Build a http string pointing to one of the active authorization service containers
		InstanceInfo authorizationService = discoveryClient.getNextServerFromEureka(
				AUTHORIZATION_SERVICE_NAME, false
		);
		String authorizationServiceIP = authorizationService.getIPAddr();
		int authorizationServicePort = authorizationService.getPort();
		String authorizationServiceURL =
				"http://" + authorizationServiceIP + ":" + authorizationServicePort + CHECK_TOKEN_PATH;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost checkTokenRequest = new HttpPost(authorizationServiceURL);
			List<NameValuePair> body = new ArrayList<>();
			body.add(new BasicNameValuePair("token", userToken));
			body.add(new BasicNameValuePair("scope", routeScope));
			checkTokenRequest.setEntity(new UrlEncodedFormEntity(body));
			checkTokenRequest.addHeader("Authorization", "Bearer " + userToken);
			CloseableHttpResponse checkTokenResponse = client.execute(checkTokenRequest);
			String responseContent = new String(
					checkTokenResponse.getEntity().getContent().readAllBytes(),
					StandardCharsets.UTF_8
			);
			JSONObject response = new JSONObject(responseContent);
			return response.getBoolean("active");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private String getAuthorizationToken(HttpHeaders headers) {
		String authorizationHeaderValue = headers.getFirst("Authorization");
		String possibleToken = authorizationHeaderValue.replaceAll("Bearer\s*", "");
		return possibleToken;
	}

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
		boolean hasTokenInUUIDFormat = false;
		try {
			UUID token = UUID.fromString(possibleToken);
		} catch (IllegalArgumentException e) {
			logger.warn("Illegal Token found. Denying access to the system");
			return false;
		}
		return hasBearerAsAuthorizationMethod;
	}
}
