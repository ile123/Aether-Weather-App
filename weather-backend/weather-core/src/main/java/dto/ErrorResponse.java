package dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        URI type,
        String title,
        Integer status,
        String detail,
        URI instance,
        String correlationId,
        Instant timestamp,
        List<ErrorItem> errors
) {
    public static final URI TYPE_VALIDATION =
            URI.create("https://weather-app.com/errors/validation-error");
    public static final URI TYPE_NOT_FOUND =
            URI.create("https://weather-app.com/errors/resource-not-found");
    public static final URI TYPE_EXTERNAL_API =
            URI.create("https://weather-app.com/errors/external-api-error");
    public static final URI TYPE_UNAUTHORIZED =
            URI.create("https://weather-app.com/errors/unauthorized");
    public static final URI TYPE_FORBIDDEN =
            URI.create("https://weather-app.com/errors/forbidden");
    public static final URI TYPE_SERVICE_UNAVAILABLE =
            URI.create("https://weather-app.com/errors/service-unavailable");
    public static final URI TYPE_INTERNAL =
            URI.create("https://weather-app.com/errors/internal-server-error");
}