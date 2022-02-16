package de.uol.wisdom.api_gateway;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * A new WebException handler which handles the changed Error Attributes
 */
@Component
@Order(Integer.MIN_VALUE + 3)
public class GlobalWebExceptionHandler extends AbstractErrorWebExceptionHandler {


	/**
	 * Create a new Exception Handler
	 * @param g The modified message contents
	 * @param applicationContext The configuration of the application
	 * @param serverCodecConfigurer Configurator for reading and writing messages to the end user
	 */
	public GlobalWebExceptionHandler(
			GlobalErrorAttributes g, ApplicationContext applicationContext,
			ServerCodecConfigurer serverCodecConfigurer) {
		super(g, new WebProperties.Resources(), applicationContext);
		super.setMessageWriters(serverCodecConfigurer.getWriters());
		super.setMessageReaders(serverCodecConfigurer.getReaders());
	}

	/**
	 * Get the routing of the error which is to be output
	 * @param errorAttributes The ErrorAttributes which were created
	 * @return A Router with a response
	 */
	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		return RouterFunctions.route(
				RequestPredicates.all(), this::renderErrorResponse
		);
	}

	/**
	 * Renderer for the modified error data
	 * @param request The request which is currently handled
	 * @return A Mono containing the response which should be sent to the end user
	 */
	private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		Map<String, Object> errorProperties = getErrorAttributes(request,
		                                                         ErrorAttributeOptions.defaults());

		return ServerResponse.status(HttpStatus.valueOf((Integer) errorProperties.get("status")))
				       .contentType(MediaType.APPLICATION_JSON)
				       .body(BodyInserters.fromValue(errorProperties));
	}
}
