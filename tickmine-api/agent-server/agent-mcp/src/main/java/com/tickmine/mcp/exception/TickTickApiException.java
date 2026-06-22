package com.tickmine.mcp.exception;

import org.springframework.http.HttpStatusCode;

public class TickTickApiException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public TickTickApiException(HttpStatusCode statusCode, String responseBody) {
        super("TickTick API error: " + statusCode + " - " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
