package de.uol.wisdom.api_gateway;

import org.json.JSONObject;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.UUID;

@Component
public class AuthorizationClient {

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private DirectExchange exchange;

    int start = 0;

    public void introspectToken(String bearer_token) {
        JSONObject request = new JSONObject();
        request.put("token", bearer_token);
        request.put("action", "validate_token");
        request.put("scope", "");
        CorrelationData data = new CorrelationData(UUID.randomUUID().toString());
        LinkedHashMap response = (LinkedHashMap) template.convertSendAndReceive(exchange.getName(), "authorization-service", request.toMap(), data);
        JSONObject result = new JSONObject(response);
        if (!result.has("active")) {
            throw new CustomResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_TOKEN_INTROSPECTION",
                    "Invalid Token Introspection Response",
                    "The internal token introspection did not return a valid result"
            );
        }
        if (!result.getBoolean("active")) {
            if (!result.has("reason")) {
                throw new CustomResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_AUTHORIZATION_INFORMATION",
                        "Invalid Authorization Information",
                        "The authorization information used to access this resource is not valid"
                );
            }
            String reason = result.getString("reason");
            switch (reason) {
                case "INVALID_TOKEN" -> throw new CustomResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_TOKEN",
                        "Invalid Access Token",
                        "The access token used to access this resource is either malformed or non-existent"
                );
                case "EXPIRED_TOKEN" -> throw new CustomResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "EXPIRED_TOKEN",
                        "Expired Access Token",
                        "The access token used to access this resource is expired"
                );
                case "USAGE_BEFORE_CREATION" -> throw new CustomResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        reason,
                        "Token used before creation",
                        "The access token used to access this resource was used before it's creation time"
                );
                case "NO_USER_ASSOCIATED" -> throw new CustomResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        reason,
                        "No user found",
                        "The access token used to access this resource has not user associated to it"
                );
                case "USER_DISABLED" -> throw new CustomResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        reason,
                        "User disabled",
                        "The user account used to access this resource currently is disabled"
                );
                default -> throw new CustomResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        reason,
                        "Unauthorized",
                        "The access token used to access this resource is not valid"
                );
            }
        }
    }
}
