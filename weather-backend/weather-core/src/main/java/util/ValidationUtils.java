package util;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class ValidationUtils {

    public void validateLocation(String location) {
        if (!StringUtils.hasText(location)) {
            throw new IllegalArgumentException("Location query cannot be empty.");
        }
        if (location.length() < 2 || location.length() > 100) {
            throw new IllegalArgumentException("Location must be between 2 and 100 characters.");
        }
    }

    public void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }
    }

    public void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
    }
}