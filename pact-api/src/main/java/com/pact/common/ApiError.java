package com.pact.common;

/**
 * Wire shape for every non-2xx response:
 * { "error": { "code": "...", "message": "..." } }
 */
public class ApiError {

    private final ErrorBody error;

    public ApiError(String code, String message) {
        this.error = new ErrorBody(code, message);
    }

    public ErrorBody getError() {
        return error;
    }

    public static class ErrorBody {
        private final String code;
        private final String message;

        public ErrorBody(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}