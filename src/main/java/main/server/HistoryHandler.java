package main.java.main.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import main.java.main.manager.TaskManager;
import main.java.main.model.Task;

import java.io.IOException;
import java.util.List;

public class HistoryHandler extends BaseHttpHandler {
    private final TaskManager taskManager;

    public HistoryHandler(TaskManager taskManager, Gson gson) {
        super(gson);
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod()) && "/history".equals(exchange.getRequestURI().getPath())) {
                handleGetHistory(exchange);
            } else {
                sendNotFound(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendInternalServerError(exchange, e.getMessage());
        }
    }

    private void handleGetHistory(HttpExchange exchange) throws IOException {
        List<Task> history = taskManager.getHistory();
        String responseJson = gson.toJson(history);
        sendText(exchange, responseJson, 200);
    }
}