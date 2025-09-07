package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.java.main.manager.InMemoryTaskManager;
import main.java.main.manager.TaskManager;
import main.java.main.manager.TaskOverlapException;
import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;
import main.java.main.server.DurationAdapter;
import main.java.main.server.HttpTaskServer;
import main.java.main.server.LocalDateTimeAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpTaskServerTest {
    private static final String BASE_URL = "http://localhost:8080";
    private HttpTaskServer server;
    private TaskManager taskManager;
    private HttpClient client;
    private Gson gson;

    @BeforeEach
    void setUp() throws IOException {
        // Создаем TaskManager и Gson с нужными адаптерами
        taskManager = new InMemoryTaskManager();
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .create();

        // Создаем и запускаем сервер с нашим TaskManager
        server = new HttpTaskServer(taskManager, gson);
        server.start();

        // Создаем HTTP клиент для тестов
        client = HttpClient.newHttpClient();

        // ВАЖНО: используем TaskManager от сервера для проверок!
        taskManager = server.getTaskManager();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    // ======================== ТЕСТЫ ДЛЯ TASKS ========================

    @Test
    void shouldCreateTask() throws IOException, InterruptedException {
        Task task = new Task("Тестовая задача", "Описание", 0, TaskStatus.NEW,
                Duration.ofHours(2), LocalDateTime.of(2025, 1, 15, 10, 0));

        String taskJson = gson.toJson(task);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");
        assertEquals(1, taskManager.getAllTasks().size(), "В менеджере должна быть одна задача");

        Task createdTask = taskManager.getAllTasks().get(0);
        assertEquals("Тестовая задача", createdTask.getTitle(), "Название задачи должно совпадать");
        assertEquals(TaskStatus.NEW, createdTask.getStatus(), "Статус задачи должен быть NEW");
    }

    @Test
    void shouldGetAllTasks() throws IOException, InterruptedException, TaskOverlapException {
        // Создаем тестовые задачи через менеджер
        Task task1 = taskManager.createTask("Задача 1", "Описание 1", TaskStatus.NEW);
        Task task2 = taskManager.createTask("Задача 2", "Описание 2", TaskStatus.IN_PROGRESS);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");

        // Проверяем, что ответ содержит JSON массив с задачами
        String responseBody = response.body();
        assertTrue(responseBody.contains("Задача 1"), "Ответ должен содержать первую задачу");
        assertTrue(responseBody.contains("Задача 2"), "Ответ должен содержать вторую задачу");
    }

    @Test
    void shouldGetTaskById() throws IOException, InterruptedException, TaskOverlapException {
        Task task = taskManager.createTask("Тестовая задача", "Описание", TaskStatus.NEW);
        int taskId = task.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");

        String responseBody = response.body();
        assertTrue(responseBody.contains("Тестовая задача"), "Ответ должен содержать название задачи");
    }

    @Test
    void shouldReturn404ForNonExistentTask() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Статус ответа должен быть 404 для несуществующей задачи");
    }

    @Test
    void shouldUpdateTask() throws IOException, InterruptedException, TaskOverlapException {
        Task task = taskManager.createTask("Оригинальная задача", "Описание", TaskStatus.NEW);
        int taskId = task.getId();

        Task updatedTask = new Task("Обновленная задача", "Новое описание", taskId, TaskStatus.DONE);
        String taskJson = gson.toJson(updatedTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");

        Task taskFromManager = taskManager.getTaskById(taskId);
        assertEquals("Обновленная задача", taskFromManager.getTitle(), "Название задачи должно быть обновлено");
        assertEquals(TaskStatus.DONE, taskFromManager.getStatus(), "Статус задачи должен быть обновлен");
    }

    @Test
    void shouldDeleteTask() throws IOException, InterruptedException, TaskOverlapException {
        Task task = taskManager.createTask("Задача для удаления", "Описание", TaskStatus.NEW);
        int taskId = task.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");
        assertEquals(0, taskManager.getAllTasks().size(), "В менеджере не должно быть задач");
    }

    @Test
    void shouldDeleteAllTasks() throws IOException, InterruptedException, TaskOverlapException {
        taskManager.createTask("Задача 1", "Описание 1", TaskStatus.NEW);
        taskManager.createTask("Задача 2", "Описание 2", TaskStatus.NEW);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");
        assertEquals(0, taskManager.getAllTasks().size(), "В менеджере не должно быть задач");
    }

    @Test
    void shouldReturn406ForOverlappingTasks() throws IOException, InterruptedException, TaskOverlapException {
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 10, 0);
        Duration duration = Duration.ofHours(2);

        // Создаем первую задачу
        taskManager.createTask("Первая задача", "Описание", TaskStatus.NEW, duration, startTime);

        // Пытаемся создать пересекающуюся задачу
        Task overlappingTask = new Task("Пересекающаяся задача", "Описание", 0, TaskStatus.NEW,
                duration, startTime.plusHours(1));

        String taskJson = gson.toJson(overlappingTask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response.statusCode(), "Статус ответа должен быть 406 для пересекающихся задач");
        assertEquals(1, taskManager.getAllTasks().size(), "В менеджере должна остаться только одна задача");
    }

    // ======================== ТЕСТЫ ДЛЯ EPICS ========================

    @Test
    void shouldCreateEpic() throws IOException, InterruptedException {
        Epic epic = new Epic("Тестовый эпик", "Описание эпика", 0);

        String epicJson = gson.toJson(epic);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");
        assertEquals(1, taskManager.getAllEpics().size(), "В менеджере должен быть один эпик");

        Epic createdEpic = taskManager.getAllEpics().get(0);
        assertEquals("Тестовый эпик", createdEpic.getTitle(), "Название эпика должно совпадать");
    }

    @Test
    void shouldGetAllEpics() throws IOException, InterruptedException {
        taskManager.addEpic("Эпик 1", "Описание 1");
        taskManager.addEpic("Эпик 2", "Описание 2");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/epics"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");

        String responseBody = response.body();
        assertTrue(responseBody.contains("Эпик 1"), "Ответ должен содержать первый эпик");
        assertTrue(responseBody.contains("Эпик 2"), "Ответ должен содержать второй эпик");
    }

    @Test
    void shouldGetEpicById() throws IOException, InterruptedException {
        taskManager.addEpic("Тестовый эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/epics/" + epicId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");

        String responseBody = response.body();
        assertTrue(responseBody.contains("Тестовый эпик"), "Ответ должен содержать название эпика");
    }

    @Test
    void shouldUpdateEpic() throws IOException, InterruptedException {
        taskManager.addEpic("Оригинальный эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        Epic updatedEpic = new Epic("Обновленный эпик", "Новое описание", epicId);
        String epicJson = gson.toJson(updatedEpic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/epics/" + epicId))
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");

        Epic epicFromManager = taskManager.getEpicById(epicId);
        assertEquals("Обновленный эпик", epicFromManager.getTitle(), "Название эпика должно быть обновлено");
    }

    @Test
    void shouldDeleteEpic() throws IOException, InterruptedException {
        taskManager.addEpic("Эпик для удаления", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/epics/" + epicId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");
        assertEquals(0, taskManager.getAllEpics().size(), "В менеджере не должно быть эпиков");
    }

    @Test
    void shouldGetSubtasksByEpicId() throws IOException, InterruptedException, TaskOverlapException {
        taskManager.addEpic("Эпик с подзадачами", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        taskManager.addSubTask("Подзадача 1", "Описание 1", epicId, TaskStatus.NEW);
        taskManager.addSubTask("Подзадача 2", "Описание 2", epicId, TaskStatus.IN_PROGRESS);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/epics/" + epicId + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");

        String responseBody = response.body();
        assertTrue(responseBody.contains("Подзадача 1"), "Ответ должен содержать первую подзадачу");
        assertTrue(responseBody.contains("Подзадача 2"), "Ответ должен содержать вторую подзадачу");
    }

    // ======================== ТЕСТЫ ДЛЯ SUBTASKS ========================

    @Test
    void shouldCreateSubTask() throws IOException, InterruptedException {
        taskManager.addEpic("Родительский эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        SubTask subTask = new SubTask("Тестовая подзадача", "Описание", 0, TaskStatus.NEW, epicId);

        String subTaskJson = gson.toJson(subTask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subTaskJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");
        assertEquals(1, taskManager.getAllSubTasks().size(), "В менеджере должна быть одна подзадача");

        SubTask createdSubTask = taskManager.getAllSubTasks().get(0);
        assertEquals("Тестовая подзадача", createdSubTask.getTitle(), "Название подзадачи должно совпадать");
        assertEquals(epicId, createdSubTask.getEpicId(), "ID эпика должен совпадать");
    }

    @Test
    void shouldGetAllSubTasks() throws IOException, InterruptedException, TaskOverlapException {
        taskManager.addEpic("Эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        taskManager.addSubTask("Подзадача 1", "Описание 1", epicId, TaskStatus.NEW);
        taskManager.addSubTask("Подзадача 2", "Описание 2", epicId, TaskStatus.IN_PROGRESS);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");

        String responseBody = response.body();
        assertTrue(responseBody.contains("Подзадача 1"), "Ответ должен содержать первую подзадачу");
        assertTrue(responseBody.contains("Подзадача 2"), "Ответ должен содержать вторую подзадачу");
    }

    @Test
    void shouldGetSubTaskById() throws IOException, InterruptedException, TaskOverlapException {
        taskManager.addEpic("Эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        taskManager.addSubTask("Тестовая подзадача", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask = taskManager.getAllSubTasks().get(0);
        int subTaskId = subTask.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/subtasks/" + subTaskId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");

        String responseBody = response.body();
        assertTrue(responseBody.contains("Тестовая подзадача"), "Ответ должен содержать название подзадачи");
    }

    @Test
    void shouldUpdateSubTask() throws IOException, InterruptedException, TaskOverlapException {
        taskManager.addEpic("Эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        taskManager.addSubTask("Оригинальная подзадача", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask = taskManager.getAllSubTasks().get(0);
        int subTaskId = subTask.getId();

        SubTask updatedSubTask = new SubTask("Обновленная подзадача", "Новое описание",
                subTaskId, TaskStatus.DONE, epicId);
        String subTaskJson = gson.toJson(updatedSubTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subTaskJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");

        SubTask subTaskFromManager = taskManager.getSubTaskById(subTaskId);
        assertEquals("Обновленная подзадача", subTaskFromManager.getTitle(), "Название подзадачи должно быть обновлено");
        assertEquals(TaskStatus.DONE, subTaskFromManager.getStatus(), "Статус подзадачи должен быть обновлен");
    }

    @Test
    void shouldDeleteSubTask() throws IOException, InterruptedException, TaskOverlapException {
        taskManager.addEpic("Эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        taskManager.addSubTask("Подзадача для удаления", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask = taskManager.getAllSubTasks().get(0);
        int subTaskId = subTask.getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/subtasks/" + subTaskId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");
        assertEquals(0, taskManager.getAllSubTasks().size(), "В менеджере не должно быть подзадач");
    }

    // ======================== ТЕСТЫ ДЛЯ HISTORY ========================

    @Test
    void shouldGetHistory() throws IOException, InterruptedException, TaskOverlapException {
        // Создаем задачи и добавляем их в историю
        Task task = taskManager.createTask("Задача", "Описание", TaskStatus.NEW);
        taskManager.addEpic("Эпик", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);

        // Обращаемся к задачам, чтобы они попали в историю
        taskManager.getTaskById(task.getId());
        taskManager.getEpicById(epic.getId());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");
        assertEquals(2, taskManager.getHistory().size(), "История должна содержать 2 элемента");
    }

    // ======================== ТЕСТЫ ДЛЯ PRIORITIZED ========================

    @Test
    void shouldGetPrioritizedTasks() throws IOException, InterruptedException, TaskOverlapException {
        LocalDateTime now = LocalDateTime.of(2025, 1, 15, 10, 0);

        taskManager.createTask("Задача 2", "Описание", TaskStatus.NEW,
                Duration.ofHours(1), now.plusHours(1));
        taskManager.createTask("Задача 1", "Описание", TaskStatus.NEW,
                Duration.ofHours(1), now);
        taskManager.createTask("Задача без времени", "Описание", TaskStatus.NEW);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Статус ответа должен быть 200");
        assertEquals(2, taskManager.getPrioritizedTasks().size(), "Приоритетный список должен содержать 2 задачи");
    }

    // ======================== ТЕСТЫ НА НЕКОРРЕКТНЫЕ ЗАПРОСЫ ========================

    @Test
    void shouldReturn404ForInvalidEndpoint() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/invalid"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Статус ответа должен быть 404 для несуществующего эндпоинта");
    }

    @Test
    void shouldReturn404ForInvalidMethod() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Статус ответа должен быть 404 для неподдерживаемого метода");
    }

    @Test
    void shouldHandleInvalidIdFormat() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/invalid"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Статус ответа должен быть 404 для некорректного формата ID");
    }

    @Test
    void shouldCreateSubTaskWithTimeFields() throws IOException, InterruptedException {
        taskManager.addEpic("Эпик с временем", "Описание");
        Epic epic = taskManager.getAllEpics().get(0);
        int epicId = epic.getId();

        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 10, 0);
        Duration duration = Duration.ofHours(2);

        SubTask subTask = new SubTask("Подзадача с временем", "Описание", 0,
                TaskStatus.NEW, epicId, duration, startTime);

        String subTaskJson = gson.toJson(subTask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subTaskJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");

        SubTask createdSubTask = taskManager.getAllSubTasks().get(0);
        assertEquals(duration, createdSubTask.getDuration(), "Продолжительность должна совпадать");
        assertEquals(startTime, createdSubTask.getStartTime(), "Время начала должно совпадать");

        // Проверяем, что подзадача попала в приоритетный список
        assertEquals(1, taskManager.getPrioritizedTasks().size(),
                "Подзадача с временем должна быть в приоритетном списке");
    }
}