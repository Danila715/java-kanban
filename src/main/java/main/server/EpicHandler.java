package main.java.main.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import main.java.main.manager.NotFoundException;
import main.java.main.manager.TaskManager;
import main.java.main.model.Epic;
import main.java.main.model.SubTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EpicHandler extends BaseHttpHandler {
    private final TaskManager taskManager;

    public EpicHandler(TaskManager taskManager, Gson gson) {
        super(gson);
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/epics")) {
                switch (method) {
                    case "GET":
                        handleGetAllEpics(exchange);
                        break;
                    case "POST":
                        handleCreateEpic(exchange);
                        break;
                    case "DELETE":
                        handleDeleteAllEpics(exchange);
                        break;
                    default:
                        sendNotFound(exchange);
                        break;
                }
            } else if (path.matches("/epics/\\d+")) {
                int epicId = getIdFromPath(path);
                switch (method) {
                    case "GET":
                        handleGetEpicById(exchange, epicId);
                        break;
                    case "POST": // Обновление эпика
                        handleUpdateEpic(exchange, epicId);
                        break;
                    case "DELETE":
                        handleDeleteEpic(exchange, epicId);
                        break;
                    default:
                        sendNotFound(exchange);
                        break;
                }
            } else if (path.matches("/epics/\\d+/subtasks")) {
                if ("GET".equals(method)) {
                    int epicId = getIdFromPath(path.substring(0, path.lastIndexOf("/"))); // Извлекаем ID эпика из части пути
                    handleGetSubtasksByEpicId(exchange, epicId);
                } else {
                    sendNotFound(exchange);
                }
            } else {
                sendNotFound(exchange);
            }
        } catch (NotFoundException e) {
            sendNotFound(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            sendInternalServerError(exchange, e.getMessage());
        }
    }

    private void handleGetAllEpics(HttpExchange exchange) throws IOException {
        List<Epic> epics = taskManager.getAllEpics();
        String responseJson = gson.toJson(epics);
        sendText(exchange, responseJson, 200);
    }

    private void handleGetEpicById(HttpExchange exchange, int id) throws IOException, NotFoundException {
        Epic epic = taskManager.getEpicById(id);
        String responseJson = gson.toJson(epic);
        sendText(exchange, responseJson, 200);
    }

    private void handleCreateEpic(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        Epic epic = gson.fromJson(body, Epic.class);
        taskManager.addEpic(epic.getTitle(), epic.getDescription());
        sendText(exchange, "Эпик создан", 201);
    }

    private void handleUpdateEpic(HttpExchange exchange, int id) throws IOException, NotFoundException {
        InputStream inputStream = exchange.getRequestBody();
        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        Epic epic = gson.fromJson(body, Epic.class);
        if (epic.getId() != id) {
            sendText(exchange, "ID в пути и теле запроса не совпадают", 400);
            return;
        }
        taskManager.updateEpic(epic);
        sendText(exchange, "Эпик обновлен", 201);
    }


    private void handleDeleteEpic(HttpExchange exchange, int id) throws IOException, NotFoundException {
        taskManager.deleteEpic(id);
        sendText(exchange, "Эпик удален", 200);
    }

    private void handleDeleteAllEpics(HttpExchange exchange) throws IOException {
        taskManager.deleteAllEpics();
        sendText(exchange, "Все эпики удалены", 200);
    }

    private void handleGetSubtasksByEpicId(HttpExchange exchange, int epicId) throws IOException, NotFoundException {
        taskManager.getEpicById(epicId);

        List<SubTask> subTasks = taskManager.getSubTasks(epicId);
        String responseJson = gson.toJson(subTasks);
        sendText(exchange, responseJson, 200);
    }

}