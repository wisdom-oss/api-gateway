package de.uol.wisdom.api_gateway.config;

import de.uol.wisdom.api_gateway.AuthorizationClient;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${CONFIG_AUTHORIZATION_EXCHANGE:authorization-service}")
    private String authorization_exchange;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(authorization_exchange, false, false);
    }

    @Bean
    public AuthorizationClient client() {
        return new AuthorizationClient();
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
