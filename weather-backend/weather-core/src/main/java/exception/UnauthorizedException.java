package exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends BaseException {
    private final String field;

    public UnauthorizedException(String field, String message) {
        super(message);
        this.field = field;
    }
}