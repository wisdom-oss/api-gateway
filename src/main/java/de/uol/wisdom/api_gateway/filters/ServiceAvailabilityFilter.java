package de.uol.wisdom.api_gateway.filters;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.converters.Auto;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;

@Component
public class ServiceAvailabilityFilter implements GlobalFilter {

    @Autowired
    private EurekaClient eurekaClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Access the request
        System.out.println("=====================================================");
        ServerHttpRequest request = exchange.getRequest();
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        URI serviceUri = route != null ? route.getUri() : null;
        String serviceName = serviceUri != null ? serviceUri.getHost() : null;
        if (serviceName != null) {
            try {
                InstanceInfo instanceInfo = eurekaClient.getNextServerFromEureka(serviceName, false);
            } catch (java.lang.RuntimeException e) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("httpCode", HttpStatus.SERVICE_UNAVAILABLE.value());
                errorResponse.put("httpError", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
                errorResponse.put("error", "gateway.NO_SERVICE_INSTANCE");
                errorResponse.put("errorName", "No active service instance");
                errorResponse.put("errorDescription", "The requested service is configured, but no instance reports the status 'UP");
                byte[] content = errorResponse.toString().getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(content);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return response.writeWith(Mono.just(buffer));
            }
        }
        return chain.filter(exchange);
    }
}

