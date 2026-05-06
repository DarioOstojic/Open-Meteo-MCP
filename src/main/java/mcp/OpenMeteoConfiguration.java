package mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rest.WeatherForecastClient;

@Configuration
public class OpenMeteoConfiguration {

    @Bean
    public WeatherForecastClient weatherForecastClient() {
        return new WeatherForecastClient();
    }
}