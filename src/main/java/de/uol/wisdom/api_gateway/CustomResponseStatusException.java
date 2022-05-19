package de.uol.wisdom.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomResponseStatusException extends ResponseStatusException {

    private String errorCode;
    private String errorTitle;
    private String errorDescription;

    public CustomResponseStatusException(HttpStatus status) {
        super(status);
    }

    public CustomResponseStatusException(HttpStatus status, String error, String title, String description) {
        super(status, error);
        this.errorCode = error;
        this.errorTitle = title;
        this.errorDescription = description;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorTitle() {
        return errorTitle;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
