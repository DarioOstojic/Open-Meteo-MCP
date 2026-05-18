# Open-Meteo-MCP

A Spring Boot MCP server that exposes weather forecast data from [Open-Meteo](https://open-meteo.com/) as an MCP tool.

## Tool

**`getWeatherForecast`** — fetches forecast data (temperature, precipitation, rain, snowfall, wind speed, sunrise/sunset) by latitude and longitude.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `latitude` | Double | Yes | Latitude of the location |
| `longitude` | Double | Yes | Longitude of the location |
| `pastDays` | Integer | No | Past days to include (default: 0) |
| `forecastDays` | Integer | No | Forecast days to include (default: 7, max: 16) |

## Running

### With Docker

```bash
docker build -t open-meteo-mcp .
docker run -p 8080:8080 -e X_API_HEADER_KEY=your-api-key open-meteo-mcp
```

### With Gradle

```bash
X_API_HEADER_KEY=your-api-key ./gradlew bootRun
```

## Authentication

Requests must include the API key via the `X-API-KEY` header. Set the key through the `X_API_HEADER_KEY` environment variable.

## MCP Endpoint

```
http://localhost:8080/mcp
```
