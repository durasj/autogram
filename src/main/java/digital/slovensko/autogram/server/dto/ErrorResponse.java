package digital.slovensko.autogram.server.dto;

import digital.slovensko.autogram.core.errors.AutogramException;
import digital.slovensko.autogram.core.errors.SigningCanceledByUserException;
import digital.slovensko.autogram.core.errors.UnrecognizedException;
import digital.slovensko.autogram.server.errors.MalformedBodyException;
import digital.slovensko.autogram.server.errors.RequestValidationException;
import digital.slovensko.autogram.server.errors.UnsupportedSignatureLevelExceptionError;

public class ErrorResponse {
    private final int statusCode;
    private final ErrorResponseBody body;

    public ErrorResponse(int statusCode, ErrorResponseBody body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public ErrorResponse(int statusCode, String code, AutogramException e) {
        this(statusCode, new ErrorResponseBody(code, e.getSubheading(), e.getDescription()));
    }

    public ErrorResponse(int statusCode, String code, String message, String details) {
        this(statusCode, new ErrorResponseBody(code, message, details));
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ErrorResponseBody getBody() {
        return body;
    }

    public static ErrorResponse buildFromException(Exception error) {
        return switch (error) {
            case SigningCanceledByUserException e -> new ErrorResponse(204, "USER_CANCELLED", e);
            case UnrecognizedException e -> new ErrorResponse(502, "UNRECOGNIZED_DSS_ERROR", e);
            case UnsupportedSignatureLevelExceptionError e -> new ErrorResponse(422, "UNSUPPORTED_SIGNATURE_LEVEL", e);
            case RequestValidationException e -> new ErrorResponse(422, "UNPROCESSABLE_INPUT", e);
            case MalformedBodyException e -> new ErrorResponse(400, "MALFORMED_INPUT", e);
            case AutogramException e -> new ErrorResponse(502, "SIGNING_FAILED", e);
            case Exception e -> new ErrorResponse(500, "INTERNAL_ERROR", "Unexpected exception signing document", e.getMessage());
        };
    }
}