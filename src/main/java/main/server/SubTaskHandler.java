package main.java.main.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import main.java.main.manager.NotFoundException;
import main.java.main.manager.TaskManager;
import main.java.main.manager.TaskOverlapException;
import main.java.main.model.SubTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubTaskHandler extends BaseHttpHandler {
    private final TaskManager taskManager;

    public SubTaskHandler(TaskManager taskManager, Gson gson) {
        super(gson);
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    if (path.equals("/subtasks")) {
                        handleGetAllSubtasks(exchange);
                    } else if (path.matches("/subtasks/\\d+")) {
                        handleGetSubtaskById(exchange, getIdFromPath(path));
                    } else {
                        sendNotFound(exchange);
                    }
                    break;
                case "POST":
                    if (path.equals("/subtasks")) {
                        handleCreateOrUpdateSubtask(exchange);
                    } else {
                        sendNotFound(exchange);
                    }
                    break;
                case "DELETE":
                    if (path.matches("/subtasks/\\d+")) {
                        handleDeleteSubtaskById(exchange, getIdFromPath(path));
                    } else if (path.equals("/subtasks")) {
                        handleDeleteAllSubtasks(exchange);
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
        } catch (Exception e) {
            e.printStackTrace();
            sendInternalServerError(exchange, e.getMessage());
        }
    }

    private void handleGetAllSubtasks(HttpExchange exchange) throws IOException {
        List<SubTask> subTasks = taskManager.getAllSubTasks();
        String responseJson = gson.toJson(subTasks);
        sendText(exchange, responseJson, 200);
    }

    private void handleGetSubtaskById(HttpExchange exchange, int id) throws IOException, NotFoundException {
        SubTask subTask = taskManager.getSubTaskById(id);
        String responseJson = gson.toJson(subTask);
        sendText(exchange, responseJson, 200);
    }

    private void handleCreateOrUpdateSubtask(HttpExchange exchange) throws IOException, TaskOverlapException, NotFoundException {
        InputStream inputStream = exchange.getRequestBody();
        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        SubTask subTask = gson.fromJson(body, SubTask.class);

        if (subTask.getId() == 0) { // Создание
            // Проверим, что эпик существует
            taskManager.getEpicById(subTask.getEpicId()); // Может выбросить NotFoundException
            taskManager.addSubTask(subTask.getTitle(), subTask.getDescription(), subTask.getEpicId(), subTask.getStatus(), subTask.getDuration(), subTask.getStartTime());
            sendText(exchange, "Подзадача создана", 201);
        } else { // Обновление
            taskManager.updateSubTask(subTask); // Может выбросить NotFoundException или TaskOverlapException
            sendText(exchange, "Подзадача обновлена", 201);
        }
    }

    private void handleDeleteSubtaskById(HttpExchange exchange, int id) throws IOException, NotFoundException {
        taskManager.deleteSubTask(id);
        sendText(exchange, "Подзадача удалена", 200);
    }

    private void handleDeleteAllSubtasks(HttpExchange exchange) throws IOException {
        taskManager.deleteAllSubTasks();
        sendText(exchange, "Все подзадачи удалены", 200);
    }
}