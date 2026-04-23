package rest;

import java.net.http.HttpClient;
import java.time.Duration;

public class WeatherForecastClient {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private final HttpClient httpClient;

    public WeatherForecastClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getForecast(
            double latitude,
            double longitude,
            int pastDays,
            int forecastDays
    ) {
        //TODO: implement by looking at the documentation at https://open-meteo.com/en/docs
        // Return the forecast as a JSON string including temperatures, precipitation, rain, snowfall, wind speed, sunrise, sunset times...
        return null;
    }
}

