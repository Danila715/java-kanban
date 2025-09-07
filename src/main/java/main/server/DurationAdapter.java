package main.java.main.server;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

// Адаптер для сериализации/десериализации Duration в/из long (минуты)
public class DurationAdapter extends TypeAdapter<Duration> {

    @Override
    public void write(JsonWriter out, Duration value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            // Записываем Duration как количество минут
            out.value(value.toMinutes());
        }
    }

    @Override
    public Duration read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            // Читаем количество минут и создаем Duration
            long minutes = in.nextLong();
            return Duration.ofMinutes(minutes);
        }
    }
}