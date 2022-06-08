package de.uol.wisdom.api_gateway;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.LinkedHashMap;
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
		Map<String, Object> errorData = new LinkedHashMap<>();
		errorData.put("httpCode", status.value());
		errorData.put("httpError", status.getReasonPhrase());
		if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
			errorData.put("error", "gateway.INTERNAL_ERROR");
			errorData.put("errorName", "Internal Error");
			errorData.put("errorDescription", map.get("exception").toString().concat(" - ").concat(map.get("message").toString()));
		} else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
			errorData.put("error", "gateway.NO_SERVICE_INSTANCE");
			errorData.put("errorName", "No active service instance");
			errorData.put("errorDescription", "The requested service is configured, but no instance reports the status 'UP'");
		} else if (status == HttpStatus.BAD_GATEWAY) {
			errorData.put("error", "gateway.ROUTING_ERROR");
			errorData.put("errorName", "Request Routing Error");
			errorData.put("errorDescription", "There is a instance of the requested service present, but the " +
					                   "instance did not answer or has problems by it self");
		} else if (status == HttpStatus.NOT_FOUND) {
			errorData.put("error", "gateway.SERVICE_NOT_CONFIGURED");
			errorData.put("errorName", "Service not set up");
			errorData.put("errorDescription", "The requested service is not configured in the gateway");
		}
		return errorData;
	}

}
