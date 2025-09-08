package com.wallway.ai_chat_poc.tools;

import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;

public class DateTimeTools {

    @Tool(description = "Get the current date and time")
    public String getCurrentDateTime(String timeZone) {
        return java.time.ZonedDateTime.now(java.time.ZoneId.of(timeZone))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}