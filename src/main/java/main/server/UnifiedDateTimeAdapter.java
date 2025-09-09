package main.java.main.server;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Универсальный адаптер для сериализации/десериализации LocalDateTime и Duration
 */
public class UnifiedDateTimeAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) src;
            return new JsonPrimitive(dateTime.format(DATE_TIME_FORMATTER));
        } else if (src instanceof Duration) {
            Duration duration = (Duration) src;
            return new JsonPrimitive(duration.toMinutes());
        }
        return null;
    }

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        if (json.isJsonNull()) {
            return null;
        }

        String value = json.getAsString();

        if (typeOfT.equals(LocalDateTime.class)) {
            if (value.isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } else if (typeOfT.equals(Duration.class)) {
            long minutes = Long.parseLong(value);
            return Duration.ofMinutes(minutes);
        }

        throw new JsonParseException("Unsupported type: " + typeOfT);
    }
}