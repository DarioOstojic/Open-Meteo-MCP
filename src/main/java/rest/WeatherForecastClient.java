package rest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.StringJoiner;

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
    ) throws IOException, InterruptedException {

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }

        if (pastDays < 0) {
            throw new IllegalArgumentException("pastDays cannot be negative.");
        }

        if (forecastDays < 1 || forecastDays > 16) {
            throw new IllegalArgumentException("forecastDays must be between 1 and 16.");
        }

        StringJoiner query = new StringJoiner("&");

        query.add(param("latitude", String.valueOf(latitude)));
        query.add(param("longitude", String.valueOf(longitude)));

        query.add(param(
                "hourly",
                "temperature_2m,precipitation,rain,snowfall,wind_speed_10m"
        ));

        query.add(param(
                "daily",
                "sunrise,sunset,temperature_2m_max,temperature_2m_min,precipitation_sum,rain_sum,snowfall_sum,wind_speed_10m_max"
        ));

        query.add(param("timezone", "auto"));
        query.add(param("past_days", String.valueOf(pastDays)));
        query.add(param("forecast_days", String.valueOf(forecastDays)));

        URI uri = URI.create(BASE_URL + "?" + query);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Open-Meteo request failed with status "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        return response.body();
    }

    private String param(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}