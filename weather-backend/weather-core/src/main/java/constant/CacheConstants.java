package constant;

public final class CacheConstants {

    private CacheConstants() {}

    public static final String WEATHER_CURRENT_CACHE = "weather:current";
    public static final String WEATHER_FORECAST_CACHE = "weather:forecast";
    public static final String USER_PREFERENCES_CACHE = "user:preferences";
    public static final String ALERT_RULES_CACHE = "alert:rules";

    public static final String WEATHER_CURRENT_KEY = "weather:current:";
    public static final String WEATHER_FORECAST_KEY = "weather:forecast:";
    public static final String USER_PREFERENCES_KEY = "user:preferences:";
    public static final String ALERT_RULES_KEY = "alert:rules:";

    public static final long WEATHER_CURRENT_TTL = 300;
    public static final long WEATHER_FORECAST_TTL = 1800;
    public static final long USER_PREFERENCES_TTL = 3600;
    public static final long ALERT_RULES_TTL = 600;
}