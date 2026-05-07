package mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import rest.WeatherForecastClient;

@Component
public class OpenMeteoTools {

    private final WeatherForecastClient client;
    private final ObjectMapper objectMapper;

    public OpenMeteoTools(WeatherForecastClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    @McpTool(description = "Get weather forecast data from Open-Meteo using latitude and longitude. Returns temperature, precipitation, rain, snowfall, wind speed, sunrise, and sunset data.")
    public Object getWeatherForecast(
            @McpToolParam(description = "Latitude of the destination, for example 41.9028 for Rome.", required = true)
            Double latitude,

            @McpToolParam(description = "Longitude of the destination, for example 12.4964 for Rome.", required = true)
            Double longitude,

            @McpToolParam(description = "Number of past days to include. Use 0 if not needed.", required = false)
            Integer pastDays,

            @McpToolParam(description = "Number of forecast days to include. Use 7 by default. Maximum 16.", required = false)
            Integer forecastDays
    ) throws Exception {

        int safePastDays = pastDays == null ? 0 : pastDays;
        int safeForecastDays = forecastDays == null ? 7 : forecastDays;

        String response = client.getForecast(
                latitude,
                longitude,
                safePastDays,
                safeForecastDays
        );

        return objectMapper.readValue(response, Object.class);
    }
}