package constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ApiConstants {
    public static final String WEATHER_API_PREFIX = "/api/weather";
    public static final String ALERTS_API_PREFIX = "/api/alerts";
    public static final String PREFERENCES_API_PREFIX = "/api/preferences";

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    public static final String JWT_ROLES_CLAIM = "realm_access";
    public static final String JWT_USER_ID_CLAIM = "sub";
    public static final String JWT_EMAIL_CLAIM = "email";
    public static final String JWT_USERNAME_CLAIM = "preferred_username";
}