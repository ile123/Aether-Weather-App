package exception;

import lombok.Getter;

@Getter
public class ExternalApiException extends BaseException {
    private final String field;

    public ExternalApiException(String field, String message) {
        super(message);
        this.field = field;
    }
}