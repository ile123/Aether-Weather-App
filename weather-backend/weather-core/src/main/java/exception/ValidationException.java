package exception;

import lombok.Getter;

@Getter
public class ValidationException extends BaseException {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}