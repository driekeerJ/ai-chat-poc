package com.wallway.ai_chat_poc.tools;

import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;

public class DateTimeTools {

    @Tool(description = "Get the current date")
    public String getCurrentDateTime(String timeZone) {
        System.out.println("[DateTimeTools] Getting current date and time for timezone: " + timeZone);
        try {
            var now = java.time.ZonedDateTime.now(java.time.ZoneId.of(timeZone));
            var formatted = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            System.out.println("[DateTimeTools] Current ZonedDateTime: " + now);
            System.out.println("[DateTimeTools] Formatted result: " + formatted);
            return formatted;
        } catch (Exception e) {
            System.out.println("[DateTimeTools] Error getting date and time for timezone: " + timeZone + ". Exception: " + e.getMessage());
            throw e;
        }
    }
}