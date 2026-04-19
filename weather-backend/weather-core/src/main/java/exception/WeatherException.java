package exception;

import lombok.Getter;

@Getter
public class WeatherException extends BaseException {
    private final String field;

    public WeatherException(String field, String message) {
        super(message);
        this.field = field;
    }
}