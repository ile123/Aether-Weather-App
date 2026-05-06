package mapper;

import java.util.Map;

public class WeatherDescriptionMapper {

    private static final Map<Integer, String> WMO_CODES = Map.of(
            0,  "Clear sky",
            1,  "Mainly clear",
            2,  "Partly cloudy",
            3,  "Overcast",
            45, "Foggy",
            61, "Light rain",
            63, "Moderate rain",
            65, "Heavy rain",
            95, "Thunderstorm"
    );

    public static String getDescription(Integer wmoCode) {
        return WMO_CODES.getOrDefault(wmoCode, "Unknown");
    }
}