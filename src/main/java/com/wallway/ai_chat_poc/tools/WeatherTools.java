package com.wallway.ai_chat_poc.tools;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

public class WeatherTools {

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool(description = """
            Get weather forecast for a city.
            IMPORTANT: If the user gives a relative date (e.g. 'zaterdag' or 'saturday', 'morgen' or 'tomorrow'),
            FIRST call getCurrentDateTime('Europe/Amsterdam') to determine today,
            THEN compute the correct ISO date (YYYY-MM-DD).
            NEVER invent a date yourself.
            """)
    public String getWeather(String city, String date) {
        try {
            System.out.println("[WeatherTools] Getting weather forecast for city: " + city + " on date: " + date);
            // Convert city to coordinates using Geocoding API
            var encodedCity = UriUtils.encode(city, StandardCharsets.UTF_8);
            var geocodingUrl = URI.create("https://geocoding-api.open-meteo.com/v1/search?name=" +
                    encodedCity + "&count=1");

            System.out.println("[WeatherTools] Calling geocoding API for city: " + city);
            var geocodingResponse = restTemplate.exchange(
                    geocodingUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            var body = geocodingResponse.getBody();
            var results = (body != null) ? (List<?>) body.getOrDefault("results", Collections.emptyList())
                    : Collections.emptyList();
            if (results.isEmpty()) {
                System.out.println("[WeatherTools] City not found in geocoding API: " + city);
                return "City not found: " + city;
            }

            var location = (Map<?, ?>) results.get(0);
            var latitude = ((Number) location.get("latitude")).doubleValue();
            var longitude = ((Number) location.get("longitude")).doubleValue();
            var cityName = (String) location.get("name");

            System.out.println("[WeatherTools] Found location for " + city + ": " + cityName + " at coordinates ["
                    + latitude + ", " + longitude + "]");

            // Get weather data from Open-Meteo API
            var weatherUrl = URI.create(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=%s&longitude=%s".formatted(latitude, longitude) +
                            "&daily=temperature_2m_max,temperature_2m_min" +
                            "&timezone=auto" +
                            "&start_date=%s&end_date=%s".formatted(date, date));

            System.out.println("[WeatherTools] Calling weather API for coordinates [" + latitude + ", " + longitude
                    + "] on date: " + date);
            var weatherResponse = restTemplate.exchange(
                    weatherUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            var weatherData = weatherResponse.getBody();
            if (weatherData == null) {
                System.out.println("[WeatherTools] Failed to retrieve weather data from API - null response");
                return "Failed to retrieve weather data";
            }

            var dailyData = (Map<?, ?>) weatherData.get("daily");
            var dailyUnits = (Map<?, ?>) weatherData.get("daily_units");

            if (dailyData == null || dailyUnits == null) {
                System.out.println("[WeatherTools] Weather data format is invalid - missing daily data or units");
                return "Weather data format is invalid";
            }

            var maxTempList = (List<?>) dailyData.get("temperature_2m_max");
            var minTempList = (List<?>) dailyData.get("temperature_2m_min");

            if (maxTempList == null || minTempList == null || maxTempList.isEmpty() || minTempList.isEmpty()) {
                System.out.println("[WeatherTools] Temperature data not available for date: " + date + " at location: "
                        + cityName);
                return "Temperature data not available for the specified date";
            }

            var maxTemp = ((Number) maxTempList.get(0)).doubleValue();
            var minTemp = ((Number) minTempList.get(0)).doubleValue();
            var unit = (String) dailyUnits.get("temperature_2m_max");

            System.out.println("[WeatherTools] Successfully retrieved weather data for " + cityName + " on " + date
                    + ": min=" + minTemp + unit + ", max=" + maxTemp + unit);

            return """
                    Weather for %s on %s:
                    Min: %.1f%s, Max: %.1f%s
                    """.formatted(cityName, date, minTemp, unit, maxTemp, unit);

        } catch (Exception e) {
            System.out.println("[WeatherTools] Error fetching weather data for city: " + city + " on date: " + date
                    + ". Exception: " + e.getMessage());
            return "Error fetching weather data: " + e.getMessage();
        }
    }
}