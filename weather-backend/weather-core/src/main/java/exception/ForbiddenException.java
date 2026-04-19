package exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends BaseException {
    private final String field;

    public ForbiddenException(String field, String message) {
        super(message);
        this.field = field;
    }
}