package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.java.main.manager.InMemoryTaskManager;
import main.java.main.manager.TaskManager;
import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;
import main.java.main.server.HttpTaskServer;
import main.java.main.server.UnifiedDateTimeAdapter;
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
                .registerTypeAdapter(LocalDateTime.class, new UnifiedDateTimeAdapter())
                .registerTypeAdapter(Duration.class, new UnifiedDateTimeAdapter())
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

    // ======================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ========================

    /**
     * Отправляет POST запрос для создания объекта
     */
    private HttpResponse<String> sendPostRequest(String endpoint, Object data) throws IOException, InterruptedException {
        String json = gson.toJson(data);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Отправляет GET запрос
     */
    private HttpResponse<String> sendGetRequest(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Отправляет DELETE запрос
     */
    private HttpResponse<String> sendDeleteRequest(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .DELETE()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Создает задачу через API и возвращает ее ID
     */
    private int createTaskViaAPI(String title, String description, TaskStatus status) throws IOException, InterruptedException {
        Task task = new Task(title, description, 0, status);
        HttpResponse<String> response = sendPostRequest("/tasks", task);
        assertEquals(201, response.statusCode());
        return taskManager.getAllTasks().get(taskManager.getAllTasks().size() - 1).getId();
    }

    /**
     * Создает эпик через API и возвращает его ID
     */
    private int createEpicViaAPI(String title, String description) throws IOException, InterruptedException {
        Epic epic = new Epic(title, description, 0);
        HttpResponse<String> response = sendPostRequest("/epics", epic);
        assertEquals(201, response.statusCode());
        return taskManager.getAllEpics().get(taskManager.getAllEpics().size() - 1).getId();
    }

    /**
     * Создает подзадачу через API и возвращает ее ID
     */
    private int createSubTaskViaAPI(String title, String description, int epicId, TaskStatus status) throws IOException, InterruptedException {
        SubTask subTask = new SubTask(title, description, 0, status, epicId);
        HttpResponse<String> response = sendPostRequest("/subtasks", subTask);
        assertEquals(201, response.statusCode());
        return taskManager.getAllSubTasks().get(taskManager.getAllSubTasks().size() - 1).getId();
    }

    // ======================== ТЕСТЫ ДЛЯ TASKS ========================

    /*
    Тест создания обычной задачи
     */
    @Test
    void shouldCreateTask() throws IOException, InterruptedException {

        Task task = new Task("Тестовая задача", "Описание", 0, TaskStatus.NEW);

        HttpResponse<String> response = sendPostRequest("/tasks", task);

        assertEquals(201, response.statusCode(), "Статус ответа должен быть 201");
        assertEquals(1, taskManager.getAllTasks().size(), "В менеджере должна быть одна задача");

        Task createdTask = taskManager.getAllTasks().get(0);
        assertEquals("Тестовая задача", createdTask.getTitle(), "Название задачи должно совпадать");
        assertEquals(TaskStatus.NEW, createdTask.getStatus(), "Статус задачи должен быть NEW");
    }

    /*
    Тест создания задачи с временными полями
     */
    @Test
    void shouldCreateTaskWithTimeFields() throws IOException, InterruptedException {

        Task task = new Task("Задача с временем", "Описание", 0, TaskStatus.NEW,
                Duration.ofHours(2), LocalDateTime.of(2025, 1, 15, 10, 0));

        HttpResponse<String> response = sendPostRequest("/tasks", task);

        assertEquals(201, response.statusCode());

        Task createdTask = taskManager.getAllTasks().get(0);
        assertEquals(Duration.ofHours(2), createdTask.getDuration());
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 0), createdTask.getStartTime());
    }

    /*
    Тест получения списка всех задач
     */
    @Test
    void shouldGetAllTasks() throws IOException, InterruptedException {

        createTaskViaAPI("Задача 1", "Описание 1", TaskStatus.NEW);
        createTaskViaAPI("Задача 2", "Описание 2", TaskStatus.IN_PROGRESS);

        HttpResponse<String> response = sendGetRequest("/tasks");

        assertEquals(200, response.statusCode());
        String responseBody = response.body();
        assertTrue(responseBody.contains("Задача 1"));
        assertTrue(responseBody.contains("Задача 2"));
    }

    /*
    Тест получения задачи по ID
     */
    @Test
    void shouldGetTaskById() throws IOException, InterruptedException {

        int taskId = createTaskViaAPI("Тестовая задача", "Описание", TaskStatus.NEW);

        HttpResponse<String> response = sendGetRequest("/tasks/" + taskId);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Тестовая задача"));
    }

    /*
    Тест получения несуществующей задачи
     */
    @Test
    void shouldReturn404ForNonExistentTask() throws IOException, InterruptedException {

        HttpResponse<String> response = sendGetRequest("/tasks/999");

        assertEquals(404, response.statusCode());
    }

    /*
    Тест обновления существующей задачи
     */
    @Test
    void shouldUpdateTask() throws IOException, InterruptedException {

        int taskId = createTaskViaAPI("Оригинальная задача", "Описание", TaskStatus.NEW);

        Task updatedTask = new Task("Обновленная задача", "Новое описание", taskId, TaskStatus.DONE);
        HttpResponse<String> response = sendPostRequest("/tasks", updatedTask);

        assertEquals(201, response.statusCode());

        Task taskFromManager = taskManager.getTaskById(taskId);
        assertEquals("Обновленная задача", taskFromManager.getTitle());
        assertEquals(TaskStatus.DONE, taskFromManager.getStatus());
    }

    /*
    Тест удаления задачи по ID
     */
    @Test
    void shouldDeleteTask() throws IOException, InterruptedException {

        int taskId = createTaskViaAPI("Задача для удаления", "Описание", TaskStatus.NEW);

        HttpResponse<String> response = sendDeleteRequest("/tasks/" + taskId);

        assertEquals(200, response.statusCode());
        assertEquals(0, taskManager.getAllTasks().size());
    }

    /*
    Тест удаления всех задач
     */
    @Test
    void shouldDeleteAllTasks() throws IOException, InterruptedException {

        createTaskViaAPI("Задача 1", "Описание 1", TaskStatus.NEW);
        createTaskViaAPI("Задача 2", "Описание 2", TaskStatus.NEW);
        assertEquals(2, taskManager.getAllTasks().size());

        HttpResponse<String> response = sendDeleteRequest("/tasks");

        assertEquals(200, response.statusCode());
        assertEquals(0, taskManager.getAllTasks().size());
    }

    /*
    Тест проверки пересечения времени выполнения задач
     */
    @Test
    void shouldReturn406ForOverlappingTasks() throws IOException, InterruptedException {

        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 10, 0);
        Duration duration = Duration.ofHours(2);

        // Создаем первую задачу
        Task task1 = new Task("Первая задача", "Описание", 0, TaskStatus.NEW, duration, startTime);
        sendPostRequest("/tasks", task1);

        // Пытаемся создать пересекающуюся задачу
        Task overlappingTask = new Task("Пересекающаяся задача", "Описание", 0, TaskStatus.NEW,
                duration, startTime.plusHours(1));
        HttpResponse<String> response = sendPostRequest("/tasks", overlappingTask);

        assertEquals(406, response.statusCode());
        assertEquals(1, taskManager.getAllTasks().size());
    }

    // ======================== ТЕСТЫ ДЛЯ EPICS ========================

    /*
    Тест создания эпика
     */
    @Test
    void shouldCreateEpic() throws IOException, InterruptedException {

        Epic epic = new Epic("Тестовый эпик", "Описание эпика", 0);

        HttpResponse<String> response = sendPostRequest("/epics", epic);

        assertEquals(201, response.statusCode());
        assertEquals("Эпик создан", response.body());
        assertEquals(1, taskManager.getAllEpics().size());

        Epic createdEpic = taskManager.getAllEpics().get(0);
        assertEquals("Тестовый эпик", createdEpic.getTitle());
    }

    /*
    Тест получения списка всех эпиков
     */
    @Test
    void shouldGetAllEpics() throws IOException, InterruptedException {

        createEpicViaAPI("Эпик 1", "Описание 1");
        createEpicViaAPI("Эпик 2", "Описание 2");

        HttpResponse<String> response = sendGetRequest("/epics");

        assertEquals(200, response.statusCode());
        String responseBody = response.body();
        assertTrue(responseBody.contains("Эпик 1"));
        assertTrue(responseBody.contains("Эпик 2"));
    }


    /*
    Тест получения эпика по ID
     */
    @Test
    void shouldGetEpicById() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Тестовый эпик", "Описание");

        HttpResponse<String> response = sendGetRequest("/epics/" + epicId);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Тестовый эпик"));
    }

    /*
    Тест обновления эпика
     */
    @Test
    void shouldUpdateEpic() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Оригинальный эпик", "Описание");

        Epic updatedEpic = new Epic("Обновленный эпик", "Новое описание", epicId);
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/epics/" + epicId))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(updatedEpic)))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        Epic epicFromManager = taskManager.getEpicById(epicId);
        assertEquals("Обновленный эпик", epicFromManager.getTitle());
        assertEquals("Новое описание", epicFromManager.getDescription());
    }

    /*
    Тест удаления эпика
     */
    @Test
    void shouldDeleteEpic() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Эпик для удаления", "Описание");

        HttpResponse<String> response = sendDeleteRequest("/epics/" + epicId);

        assertEquals(200, response.statusCode());
        assertEquals(0, taskManager.getAllEpics().size());
    }

    /*
    Тест получения подзадач эпика
     */
    @Test
    void shouldGetSubtasksByEpicId() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Эпик с подзадачами", "Описание");
        createSubTaskViaAPI("Подзадача 1", "Описание 1", epicId, TaskStatus.NEW);
        createSubTaskViaAPI("Подзадача 2", "Описание 2", epicId, TaskStatus.IN_PROGRESS);

        HttpResponse<String> response = sendGetRequest("/epics/" + epicId + "/subtasks");

        assertEquals(200, response.statusCode());
        String responseBody = response.body();
        assertTrue(responseBody.contains("Подзадача 1"));
        assertTrue(responseBody.contains("Подзадача 2"));
    }

    // ======================== ТЕСТЫ ДЛЯ SUBTASKS ========================

    /*
    Тест создания подзадачи
     */
    @Test
    void shouldCreateSubTask() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Родительский эпик", "Описание");

        SubTask subTask = new SubTask("Тестовая подзадача", "Описание", 0, TaskStatus.NEW, epicId);
        HttpResponse<String> response = sendPostRequest("/subtasks", subTask);

        assertEquals(201, response.statusCode());
        assertEquals(1, taskManager.getAllSubTasks().size());

        SubTask createdSubTask = taskManager.getAllSubTasks().get(0);
        assertEquals("Тестовая подзадача", createdSubTask.getTitle());
        assertEquals(epicId, createdSubTask.getEpicId());
    }

    /*
    Тест создания подзадачи с временными полями
     */
    @Test
    void shouldCreateSubTaskWithTimeFields() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Эпик с временем", "Описание");

        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 10, 0);
        Duration duration = Duration.ofHours(2);

        SubTask subTask = new SubTask("Подзадача с временем", "Описание", 0,
                TaskStatus.NEW, epicId, duration, startTime);

        HttpResponse<String> response = sendPostRequest("/subtasks", subTask);

        assertEquals(201, response.statusCode());

        SubTask createdSubTask = taskManager.getAllSubTasks().get(0);
        assertEquals(duration, createdSubTask.getDuration());
        assertEquals(startTime, createdSubTask.getStartTime());

        // Проверяем, что подзадача попала в приоритетный список
        assertEquals(1, taskManager.getPrioritizedTasks().size());
    }

    /*
    Тест получения списка всех подзадач
     */
    @Test
    void shouldGetAllSubTasks() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Эпик", "Описание");
        createSubTaskViaAPI("Подзадача 1", "Описание 1", epicId, TaskStatus.NEW);
        createSubTaskViaAPI("Подзадача 2", "Описание 2", epicId, TaskStatus.IN_PROGRESS);

        HttpResponse<String> response = sendGetRequest("/subtasks");

        assertEquals(200, response.statusCode());
        String responseBody = response.body();
        assertTrue(responseBody.contains("Подзадача 1"));
        assertTrue(responseBody.contains("Подзадача 2"));
    }

    /*
    Тест получения подзадачи по ID
     */
    @Test
    void shouldGetSubTaskById() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Эпик", "Описание");
        int subTaskId = createSubTaskViaAPI("Тестовая подзадача", "Описание", epicId, TaskStatus.NEW);

        HttpResponse<String> response = sendGetRequest("/subtasks/" + subTaskId);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Тестовая подзадача"));
    }

    /*
    Тест обновления подзадачи
     */
    @Test
    void shouldUpdateSubTask() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Эпик", "Описание");
        int subTaskId = createSubTaskViaAPI("Оригинальная подзадача", "Описание", epicId, TaskStatus.NEW);

        SubTask updatedSubTask = new SubTask("Обновленная подзадача", "Новое описание",
                subTaskId, TaskStatus.DONE, epicId);
        HttpResponse<String> response = sendPostRequest("/subtasks", updatedSubTask);

        assertEquals(201, response.statusCode());

        SubTask subTaskFromManager = taskManager.getSubTaskById(subTaskId);
        assertEquals("Обновленная подзадача", subTaskFromManager.getTitle());
        assertEquals(TaskStatus.DONE, subTaskFromManager.getStatus());
    }

    /*
    Тест удаления подзадачи
     */
    @Test
    void shouldDeleteSubTask() throws IOException, InterruptedException {

        int epicId = createEpicViaAPI("Эпик", "Описание");
        int subTaskId = createSubTaskViaAPI("Подзадача для удаления", "Описание", epicId, TaskStatus.NEW);

        HttpResponse<String> response = sendDeleteRequest("/subtasks/" + subTaskId);

        assertEquals(200, response.statusCode());
        assertEquals(0, taskManager.getAllSubTasks().size());
    }

    // ======================== ТЕСТЫ ДЛЯ HISTORY ========================

    /*
    Тест получения истории просмотров
     */
    @Test
    void shouldGetHistory() throws IOException, InterruptedException {

        int taskId = createTaskViaAPI("Задача", "Описание", TaskStatus.NEW);
        int epicId = createEpicViaAPI("Эпик", "Описание");

        // Обращаемся к задачам, чтобы они попали в историю
        sendGetRequest("/tasks/" + taskId);
        sendGetRequest("/epics/" + epicId);

        HttpResponse<String> response = sendGetRequest("/history");

        assertEquals(200, response.statusCode());
        assertEquals(2, taskManager.getHistory().size());
    }

    // ======================== ТЕСТЫ ДЛЯ PRIORITIZED ========================

    /*
    Тест получения приоритетного списка задач
     */
    @Test
    void shouldGetPrioritizedTasks() throws IOException, InterruptedException {

        LocalDateTime now = LocalDateTime.of(2025, 1, 15, 10, 0);

        Task task1 = new Task("Задача 1", "Описание", 0, TaskStatus.NEW, Duration.ofHours(1), now);
        Task task2 = new Task("Задача 2", "Описание", 0, TaskStatus.NEW, Duration.ofHours(1), now.plusHours(1));
        Task taskWithoutTime = new Task("Задача без времени", "Описание", 0, TaskStatus.NEW);

        sendPostRequest("/tasks", task2);  // Создаем вторую задачу первой
        sendPostRequest("/tasks", task1);  // Создаем первую задачу второй
        sendPostRequest("/tasks", taskWithoutTime);

        HttpResponse<String> response = sendGetRequest("/prioritized");

        assertEquals(200, response.statusCode());
        assertEquals(2, taskManager.getPrioritizedTasks().size()); // Только задачи с временем
    }

    // ======================== ТЕСТЫ НА НЕКОРРЕКТНЫЕ ЗАПРОСЫ ========================

    /*
    Тест обращения к несуществующему эндпоинту
     */
    @Test
    void shouldReturn404ForInvalidEndpoint() throws IOException, InterruptedException {

        HttpResponse<String> response = sendGetRequest("/invalid");

        assertEquals(404, response.statusCode());
    }

    /*
    Тест использования неподдерживаемого HTTP метода
     */
    @Test
    void shouldReturn404ForInvalidMethod() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    /*
    Тест обращения с некорректным форматом ID
     */
    @Test
    void shouldHandleInvalidIdFormat() throws IOException, InterruptedException {

        HttpResponse<String> response = sendGetRequest("/tasks/invalid");

        assertEquals(404, response.statusCode());
    }
}