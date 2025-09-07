package main.java.main.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import main.java.main.manager.Managers;
import main.java.main.manager.TaskManager;


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.time.Duration;
import java.time.LocalDateTime;

public class HttpTaskServer {
    private static final int PORT = 8080;
    private final HttpServer server;
    private final TaskManager taskManager;
    private final Gson gson;

    public HttpTaskServer() throws IOException {
        // Инициализация TaskManager (например, FileBackedTaskManager)
        // Убедитесь, что файл существует или создайте его
        File dataFile = new File("tasks.csv");
        this.taskManager = Managers.getDefaultFileBacked(dataFile); // Или getDefault() для InMemory

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()) // Ваш существующий адаптер
                .registerTypeAdapter(Duration.class, new DurationAdapter()) // НОВЫЙ адаптер для Duration
                .create();

        // Создание сервера
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Регистрация обработчиков
        server.createContext("/tasks", new TaskHandler(taskManager, gson));
        server.createContext("/subtasks", new SubTaskHandler(taskManager, gson));
        server.createContext("/epics", new EpicHandler(taskManager, gson));
        server.createContext("/history", new HistoryHandler(taskManager, gson));
        server.createContext("/prioritized", new PrioritizedHandler(taskManager, gson));

        // Добавим простой обработчик для корневого пути
        server.createContext("/", (exchange) -> {
            String response = "Task Server is running. Available endpoints: /tasks, /subtasks, /epics, /history, /prioritized";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
    }

    public void start() {
        System.out.println("Запуск сервера на порту " + PORT);
        server.start();
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public static void main(String[] args) {
        try {
            HttpTaskServer server = new HttpTaskServer();
            server.start();
            // Сервер будет работать до принудительной остановки (Ctrl+C)
        } catch (IOException e) {
            System.err.println("Ошибка при запуске сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }
}