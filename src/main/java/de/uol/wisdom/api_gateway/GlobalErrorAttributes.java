package de.uol.wisdom.api_gateway;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;


/**
 * GlobalErrorAttributes
 *
 * These attributes are used to modify the error responses this gateway sends to the end users
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

	/**
	 * Get the modified error attributes
	 *
	 * @param request The request in which the error occurred
	 * @param options The already set error attributes
	 * @return A mapping between JSON Keys and their values
	 */
	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request,
	                                              ErrorAttributeOptions options) {
		Map<String, Object> map = super.getErrorAttributes(request, options);
		HttpStatus status = HttpStatus.valueOf((Integer) map.get("status"));
		// Set the generated Request ID as error
		map.remove("requestId");
		if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
			map.put("error", "internal_gateway_error");
			map.put("message", "The Gateway experienced an internal error, which is not specified" +
					                   " further");
		} else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
			map.put("error", "no_service_instance_present");
			map.put("message", "There is no instance of the requested service present.");
		} else if (status == HttpStatus.BAD_GATEWAY) {
			map.put("error", "unable_to_route_request");
			map.put("message", "There is a instance of the requested service present, but the " +
					                   "instance did not answer or has problems by it self");
		} else if (status == HttpStatus.NOT_FOUND) {
			map.put("error", "service_not_configured");
			map.put("message", "The requested service is not configured in the gateway");
			map.put("status", 501);
		}
		return map;
	}

}
