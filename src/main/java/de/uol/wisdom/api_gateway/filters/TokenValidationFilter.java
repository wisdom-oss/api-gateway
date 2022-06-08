package de.uol.wisdom.api_gateway.filters;

import de.uol.wisdom.api_gateway.AuthorizationClient;
import de.uol.wisdom.api_gateway.CustomResponseStatusException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;

/**
 * A Global Filter validating if the bearer token present is currently active and valid
 */
@Component
public class TokenValidationFilter implements GlobalFilter {


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

    @Autowired
    AuthorizationClient client;


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
            // Get the route id to exempt the requests going to the auth service from the token
            Route routeAttributes = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            String routeId = routeAttributes != null ? routeAttributes.getId() : "NaN";
            PathPattern matcher = new PathPatternParser().parse("/auth/**");
            boolean pathMatchesAuthService = matcher.matches(request.getPath().pathWithinApplication());
            if (routeId.equals("authorization-service") || pathMatchesAuthService) {
                return chain.filter(exchange);
            }
            if (!validTokenType(headers)) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("httpCode", HttpStatus.BAD_REQUEST.value());
                errorResponse.put("httpError", HttpStatus.BAD_REQUEST.getReasonPhrase());
                errorResponse.put("error", "gateway.INVALID_TOKEN_TYPE");
                errorResponse.put("errorName", "Invalid Authorization Token type");
                errorResponse.put("errorDescription", "The token type indicated in the Authorization header is not supported");
                byte[] content = errorResponse.toString().getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(content);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return response.writeWith(Mono.just(buffer));
            }

            String accessToken = getAccessToken(headers);
            try {
                client.introspectToken(accessToken);
            } catch (CustomResponseStatusException e) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(e.getStatus());
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("httpCode", e.getStatus().value());
                errorResponse.put("httpError", e.getStatus().getReasonPhrase());
                errorResponse.put("error", "gateway.".concat(e.getErrorCode()));
                errorResponse.put("errorName", e.getErrorTitle());
                errorResponse.put("errorDescription", e.getErrorDescription());
                byte[] content = errorResponse.toString().getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(content);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return response.writeWith(Mono.just(buffer));
            }
            return chain.filter(exchange);
        }
        return chain.filter(exchange);
    }


    /**
     * Get the authorization token from the headers
     * @param headers HttpHeaders
     * @return An empty string if there is no value else the token
     */
    private String getAccessToken(HttpHeaders headers) {
        String authorizationHeaderValue = headers.getFirst("Authorization");
        if (authorizationHeaderValue != null)
            return authorizationHeaderValue.replaceAll("Bearer\s*", "");
        else
            return "";
    }

    private boolean validTokenType(HttpHeaders headers) {
        // Get the Authorization header
        String authorizationHeaderValue = headers.getFirst("Authorization");
        if (authorizationHeaderValue == null) {
            return false;
        }
        return authorizationHeaderValue.contains("Bearer");
    }
}
