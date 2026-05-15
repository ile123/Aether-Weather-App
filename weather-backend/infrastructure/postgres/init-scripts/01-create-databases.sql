oCREATE
DATABASE keycloak;
CREATE
DATABASE weather_db;
CREATE
DATABASE alert_db;
CREATE
DATABASE preferences_db;

CREATE
USER keycloak WITH PASSWORD 'keycloak';
CREATE
USER weather_user WITH PASSWORD 'weather_pass';
CREATE
USER alert_user WITH PASSWORD 'alert_pass';
CREATE
USER preferences_user WITH PASSWORD 'preferences_pass';

GRANT ALL PRIVILEGES ON DATABASE
keycloak TO keycloak;
GRANT ALL PRIVILEGES ON DATABASE
weather_db TO weather_user;
GRANT ALL PRIVILEGES ON DATABASE
alert_db TO alert_user;
GRANT ALL PRIVILEGES ON DATABASE
preferences_db TO preferences_user;

\c
keycloak
GRANT ALL ON SCHEMA public TO keycloak;

\c
weather_db
GRANT ALL ON SCHEMA public TO weather_user;

\c
alert_db
GRANT ALL ON SCHEMA public TO alert_user;

\c
preferences_db
GRANT ALL ON SCHEMA public TO preferences_user;