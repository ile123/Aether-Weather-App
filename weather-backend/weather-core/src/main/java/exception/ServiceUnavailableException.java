package exception;

import lombok.Getter;

@Getter
public class ServiceUnavailableException extends BaseException {
    private final String field;

    public ServiceUnavailableException(String field, String message) {
        super(message);
        this.field = field;
    }
}