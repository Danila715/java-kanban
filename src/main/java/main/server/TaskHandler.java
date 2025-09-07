package main.java.main.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import main.java.main.manager.NotFoundException;
import main.java.main.manager.TaskManager;
import main.java.main.manager.TaskOverlapException;
import main.java.main.model.Task;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class TaskHandler extends BaseHttpHandler {
    private final TaskManager taskManager;

    public TaskHandler(TaskManager taskManager, Gson gson) {
        super(gson);
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath(); // Например, /tasks или /tasks/1

            switch (method) {
                case "GET":
                    if (path.equals("/tasks")) {
                        handleGetAllTasks(exchange);
                    } else if (path.matches("/tasks/\\d+")) {
                        handleGetTaskById(exchange, getIdFromPath(path));
                    } else {
                        sendNotFound(exchange);
                    }
                    break;
                case "POST":
                    if (path.equals("/tasks")) {
                        handleCreateOrUpdateTask(exchange);
                    } else {
                        sendNotFound(exchange);
                    }
                    break;
                case "DELETE":
                    if (path.matches("/tasks/\\d+")) {
                        handleDeleteTaskById(exchange, getIdFromPath(path));
                    } else if (path.equals("/tasks")) {
                        handleDeleteAllTasks(exchange);
                    } else {
                        sendNotFound(exchange);
                    }
                    break;
                default:
                    sendNotFound(exchange);
                    break;
            }
        } catch (NotFoundException e) {
            sendNotFound(exchange);
        } catch (TaskOverlapException e) {
            sendHasInteraction(exchange);
        } catch (Exception e) { // Ловим все остальные исключения
            e.printStackTrace(); // Логируем для отладки
            sendInternalServerError(exchange, e.getMessage());
        }
    }

    private void handleGetAllTasks(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getAllTasks();
        String responseJson = gson.toJson(tasks);
        sendText(exchange, responseJson, 200);
    }

    private void handleGetTaskById(HttpExchange exchange, int id) throws IOException, NotFoundException {
        Task task = taskManager.getTaskById(id);
        String responseJson = gson.toJson(task);
        sendText(exchange, responseJson, 200);
    }

    private void handleCreateOrUpdateTask(HttpExchange exchange) throws IOException, TaskOverlapException {
        InputStream inputStream = exchange.getRequestBody();
        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        Task task = gson.fromJson(body, Task.class);

        if (task.getId() == 0) { // Создание новой задачи
            // Предполагаем, что TaskManager сам генерирует ID или мы передаем 0 и он его заменит
            // Если TaskManager требует ID, нужно адаптировать логику
            Task createdTask = taskManager.createTask(task.getTitle(), task.getDescription(), task.getStatus(), task.getDuration(), task.getStartTime());
            String responseJson = gson.toJson(createdTask);
            sendText(exchange, responseJson, 201);
        } else { // Обновление существующей задачи
            taskManager.updateTask(task); // Может выбросить NotFoundException или TaskOverlapException
            sendText(exchange, "Задача обновлена", 201);
        }
    }

    private void handleDeleteTaskById(HttpExchange exchange, int id) throws IOException, NotFoundException {
        taskManager.deleteTaskById(id);
        sendText(exchange, "Задача удалена", 200);
    }

    private void handleDeleteAllTasks(HttpExchange exchange) throws IOException {
        taskManager.deleteAllTasks();
        sendText(exchange, "Все задачи удалены", 200);
    }
}