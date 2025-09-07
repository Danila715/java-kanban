package main.java.main.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected final Gson gson;

    public BaseHttpHandler(Gson gson) {
        this.gson = gson;
    }

    protected void sendText(HttpExchange h, String text, int responseCode) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(responseCode, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    protected void sendNotFound(HttpExchange h) throws IOException {
        sendText(h, "Запрашиваемый ресурс не найден", 404);
    }

    protected void sendHasInteraction(HttpExchange h) throws IOException {
        sendText(h, "Задача пересекается по времени с существующими", 406);
    }

    protected void sendInternalServerError(HttpExchange h, String message) throws IOException {
        sendText(h, "Внутренняя ошибка сервера: " + message, 500);
    }

    protected int getIdFromPath(String path) throws NumberFormatException {
        String[] parts = path.split("/");
        if (parts.length > 2) {
            return Integer.parseInt(parts[2]);
        }
        return -1;
    }
}