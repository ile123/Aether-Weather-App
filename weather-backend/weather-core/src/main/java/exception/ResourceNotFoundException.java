package exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends BaseException {
    private final String field;

    public ResourceNotFoundException(String field, String message) {
        super(message);
        this.field = field;
    }
}