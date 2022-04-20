package de.uol.wisdom.api_gateway;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.HashMap;
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
		StringBuilder httpErrorName = new StringBuilder(status.name());
		String httpErrorNameSpaced = httpErrorName.toString().replace('_', ' ');
		httpErrorName = new StringBuilder();
		for (String part: httpErrorNameSpaced.split("\\s")) {
			String firstLetter = part.substring(0,1);
			String rest = part.substring(1);
			httpErrorName.append(firstLetter.toUpperCase()).append(rest.toLowerCase()).append(" ");
		}
		errorData.put("httpError", httpErrorName.toString().trim());
		if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
			errorData.put("error", "gateway.INTERNAL_ERROR");
			errorData.put("errorName", "Generic Internal Error");
			errorData.put("errorDescription", "The API Gateway experienced an internal error, which has not been handled correctly");
		} else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
			errorData.put("error", "gateway.NO_SERVICE_INSTANCE");
			errorData.put("errorName", "No service instance up");
			errorData.put("errorDescription", "The requested service is configured but has no active instances registered");
		} else if (status == HttpStatus.BAD_GATEWAY) {
			errorData.put("error", "gateway.ROUTING_ERROR");
			errorData.put("errorName", "Request Routing Error");
			errorData.put("errorDescription", "The request could not be routed to one of the instances of the service, but there are active instances");
		} else if (status == HttpStatus.NOT_FOUND) {
			errorData.put("error", "gateway.NO_SUCH_SERVICE");
			errorData.put("errorName", "Service not configured");
			errorData.put("errorDescription", "The requested service is not configured on this gateway");
		}
		return errorData;
	}

}
