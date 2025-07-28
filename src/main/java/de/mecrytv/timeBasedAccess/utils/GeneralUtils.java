package de.mecrytv.timeBasedAccess.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralUtils {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhms])");

    public static UUID getUUIDFromName(String playerName) {
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("Spieler nicht gefunden oder API‑Limit erreicht (Status "
                        + response.statusCode() + ")");
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String rawUuid = json.get("id").getAsString();

            String formatted = rawUuid.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            );

            return UUID.fromString(formatted);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Abrufen der UUID: " + e.getMessage(), e);
        }
    }

    public static LocalDateTime[] parseTime(String timeInput) {
        Matcher matcher = TIME_PATTERN.matcher(timeInput.toLowerCase());

        if (!matcher.matches()) {
            return null;
        }

        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;

        switch (unit) {
            case "d": // Tage
                endTime = startTime.plusDays(amount);
                break;
            case "h": // Stunden
                endTime = startTime.plusHours(amount);
                break;
            case "m": // Minuten
                endTime = startTime.plusMinutes(amount);
                break;
            case "s": // Sekunden
                endTime = startTime.plusSeconds(amount);
                break;
            default:
                return null;
        }

        return new LocalDateTime[]{startTime, endTime};
    }

    public static LocalDateTime[] parseComplexTime(String timeInput) {
        Matcher matcher = TIME_PATTERN.matcher(timeInput.toLowerCase());

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime;

        boolean foundMatch = false;

        while (matcher.find()) {
            foundMatch = true;
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "d":
                    endTime = endTime.plusDays(amount);
                    break;
                case "h":
                    endTime = endTime.plusHours(amount);
                    break;
                case "m":
                    endTime = endTime.plusMinutes(amount);
                    break;
                case "s":
                    endTime = endTime.plusSeconds(amount);
                    break;
            }
        }

        return foundMatch ? new LocalDateTime[]{startTime, endTime} : null;
    }
}
