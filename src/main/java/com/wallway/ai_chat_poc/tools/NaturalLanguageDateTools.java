package com.wallway.ai_chat_poc.tools;

import org.springframework.ai.tool.annotation.Tool;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NaturalLanguageDateTools {

    private static final ZoneId NETHERLANDS_TIMEZONE = ZoneId.of("Europe/Amsterdam");
    private static final Locale DUTCH_LOCALE = Locale.forLanguageTag("nl");
    private static final String NEXT_WEEK_PREFIX = "volgende week ";

    @Tool(description = "Converts Dutch natural language date to ISO date (YYYY-MM-DD). Supports: vandaag, morgen, overmorgen")
    public String parseDutchDateToIso(String naturalLanguageDate) {
        logParsingStart(naturalLanguageDate);
        
        try {
            LocalDate today = getCurrentDate();
            String normalizedInput = normalizeInput(naturalLanguageDate);
            
            LocalDate parsedDate = parseNaturalLanguageDate(normalizedInput, today);
            
            logParsingSuccess(parsedDate);
            return parsedDate.toString();
            
        } catch (Exception e) {
            logParsingError(naturalLanguageDate, e);
            throw e;
        }
    }

    private LocalDate parseNaturalLanguageDate(String normalizedInput, LocalDate today) {
        if (isRelativeDay(normalizedInput)) {
            return parseRelativeDay(normalizedInput, today);
        }
        
        if (isWeekday(normalizedInput)) {
            return parseNextWeekday(normalizedInput, today);
        }
        
        if (isNextWeekExpression(normalizedInput)) {
            return parseNextWeekDay(normalizedInput, today);
        }
        
        if (isIsoDate(normalizedInput)) {
            return LocalDate.parse(normalizedInput, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        return handleUnknownDate(normalizedInput);
    }

    private LocalDate parseRelativeDay(String input, LocalDate today) {
        return switch (input) {
            case "vandaag" -> today;
            case "morgen" -> today.plusDays(1);
            case "overmorgen" -> today.plusDays(2);
            default -> throw new IllegalArgumentException("Unknown relative day: " + input);
        };
    }

    private LocalDate parseNextWeekday(String weekdayName, LocalDate today) {
        DayOfWeek targetDayOfWeek = parseDutchWeekday(weekdayName);
        return calculateNextOccurrence(today, targetDayOfWeek);
    }

    private LocalDate parseNextWeekDay(String nextWeekExpression, LocalDate today) {
        String weekdayName = extractWeekdayFromNextWeekExpression(nextWeekExpression);
        DayOfWeek targetDayOfWeek = parseDutchWeekday(weekdayName);
        return calculateNextWeekOccurrence(today, targetDayOfWeek);
    }

    private LocalDate calculateNextOccurrence(LocalDate today, DayOfWeek targetDayOfWeek) {
        int daysAhead = calculateDaysAhead(today.getDayOfWeek(), targetDayOfWeek);
        return today.plusDays(daysAhead);
    }

    private LocalDate calculateNextWeekOccurrence(LocalDate today, DayOfWeek targetDayOfWeek) {
        int daysToNextWeek = calculateDaysAhead(today.getDayOfWeek(), targetDayOfWeek) + 7;
        return today.plusDays(daysToNextWeek);
    }

    private int calculateDaysAhead(DayOfWeek currentDay, DayOfWeek targetDay) {
        int daysAhead = Math.floorMod(targetDay.getValue() - currentDay.getValue(), 7);
        return daysAhead == 0 ? 7 : daysAhead;
    }

    private DayOfWeek parseDutchWeekday(String weekdayName) {
        return switch (weekdayName) {
            case "maandag" -> DayOfWeek.MONDAY;
            case "dinsdag" -> DayOfWeek.TUESDAY;
            case "woensdag" -> DayOfWeek.WEDNESDAY;
            case "donderdag" -> DayOfWeek.THURSDAY;
            case "vrijdag" -> DayOfWeek.FRIDAY;
            case "zaterdag" -> DayOfWeek.SATURDAY;
            case "zondag" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Unknown weekday: " + weekdayName);
        };
    }

    private boolean isRelativeDay(String input) {
        return input.equals("vandaag") || input.equals("morgen") || input.equals("overmorgen");
    }

    private boolean isWeekday(String input) {
        return switch (input) {
            case "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag", "zaterdag", "zondag" -> true;
            default -> false;
        };
    }

    private boolean isNextWeekExpression(String input) {
        return input.startsWith(NEXT_WEEK_PREFIX);
    }

    private boolean isIsoDate(String input) {
        try {
            LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractWeekdayFromNextWeekExpression(String nextWeekExpression) {
        return nextWeekExpression.replace(NEXT_WEEK_PREFIX, "");
    }

    private LocalDate handleUnknownDate(String input) {
        logUnknownDate(input);
        throw new IllegalArgumentException("Unknown date format: " + input);
    }

    private LocalDate getCurrentDate() {
        return ZonedDateTime.now(NETHERLANDS_TIMEZONE).toLocalDate();
    }

    private String normalizeInput(String input) {
        return input.trim().toLowerCase(DUTCH_LOCALE);
    }

    private void logParsingStart(String input) {
        System.out.println("[NaturalLanguageDateTools] Parsing: '" + input + "'");
    }

    private void logParsingSuccess(LocalDate result) {
        System.out.println("[NaturalLanguageDateTools] Successfully parsed to: " + result);
    }

    private void logParsingError(String input, Exception e) {
        System.out.println("[NaturalLanguageDateTools] Error parsing: " + input + ". Exception: " + e.getMessage());
    }

    private void logUnknownDate(String input) {
        System.out.println("[NaturalLanguageDateTools] Unknown date format: " + input);
    }
}