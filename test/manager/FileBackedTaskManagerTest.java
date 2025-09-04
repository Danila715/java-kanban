package manager;

import main.java.main.manager.FileBackedTaskManager;
import main.java.main.manager.InMemoryTaskManager;
import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileBackedTaskManagerTest {
    private File tempFile;

    /*
    Создаем временный файл перед каждым тестом
     */
    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("tasks", ".csv");
    }

    /*
    Тест для сохранения и загрузки пустого файла
     */
    @Test
    void saveAndLoadEmptyFile() {
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);
        manager.save();

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertEquals(0, loadedManager.getAllTasks().size(), "Список задач должен быть пустым");
        assertEquals(0, loadedManager.getAllEpics().size(), "Список эпиков должен быть пустым");
        assertEquals(0, loadedManager.getAllSubTasks().size(), "Список подзадач должен быть пустым");
    }

    /*
    Тест для сохранения нескольких задач, эпиков и подзадач
     */
    @Test
    void saveMultipleTasks() {
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);
        Task task = manager.createTask("Задача 1", "Описание задачи 1", TaskStatus.NEW);
        manager.addEpic("Эпик 1", "Описание эпика 1");
        manager.addSubTask("Подзадача 1", "Описание подзадачи 1", 2, TaskStatus.DONE);

        // Проверяем содержимое файла
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String header = reader.readLine();
            String taskLine = reader.readLine();
            String epicLine = reader.readLine();
            String subTaskLine = reader.readLine();

            assertEquals("id,type,name,status,description,epic", header, "Заголовок CSV должен быть корректным");
            assertEquals("1,TASK,Задача 1,NEW,Описание задачи 1,", taskLine, "Задача должна быть сохранена в CSV");
            assertEquals("2,EPIC,Эпик 1,DONE,Описание эпика 1,", epicLine, "Эпик должен быть сохранен в CSV");
            assertEquals("3,SUBTASK,Подзадача 1,DONE,Описание подзадачи 1,2", subTaskLine, "Подзадача должна быть сохранена в CSV");
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    /*
    Тест для загрузки нескольких задач, эпиков и подзадач
     */
    @Test
    void loadMultipleTasks() {
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);
        Task task = manager.createTask("Задача 1", "Описание задачи 1", TaskStatus.NEW);
        manager.addEpic("Эпик 1", "Описание эпика 1");
        manager.addSubTask("Подзадача 1", "Описание подзадачи 1", 2, TaskStatus.DONE);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        List<Task> tasks = loadedManager.getAllTasks();
        List<Epic> epics = loadedManager.getAllEpics();
        List<SubTask> subTasks = loadedManager.getAllSubTasks();

        assertEquals(1, tasks.size(), "Должна быть загружена одна задача");
        assertEquals("Задача 1", tasks.get(0).getTitle(), "Название задачи должно быть корректным");
        assertEquals(TaskStatus.NEW, tasks.get(0).getStatus(), "Статус задачи должен быть NEW");

        assertEquals(1, epics.size(), "Должен быть загружен один эпик");
        assertEquals("Эпик 1", epics.get(0).getTitle(), "Название эпика должно быть корректным");

        assertEquals(1, subTasks.size(), "Должна быть загружена одна подзадача");
        assertEquals("Подзадача 1", subTasks.get(0).getTitle(), "Название подзадачи должно быть корректным");
        assertEquals(TaskStatus.DONE, subTasks.get(0).getStatus(), "Статус подзадачи должен быть DONE");
        assertEquals(2, subTasks.get(0).getEpicId(), "ID эпика подзадачи должен быть корректным");
    }

    /*
    Тест для проверки поведения как у InMemoryTaskManager при создании и получении задач
     */
    @Test
    void behavesLikeInMemoryTaskManagerForTaskCreationAndRetrieval() {
        FileBackedTaskManager fileManager = new FileBackedTaskManager(tempFile);
        InMemoryTaskManager memoryManager = new InMemoryTaskManager();

        // Создаем задачи, эпики и подзадачи в обоих менеджерах
        fileManager.createTask("Задача 1", "Описание", TaskStatus.NEW);
        memoryManager.createTask("Задача 1", "Описание", TaskStatus.NEW);
        fileManager.addEpic("Эпик 1", "Описание эпика");
        memoryManager.addEpic("Эпик 1", "Описание эпика");
        fileManager.addSubTask("Подзадача 1", "Описание подзадачи", 2, TaskStatus.DONE);
        memoryManager.addSubTask("Подзадача 1", "Описание подзадачи", 2, TaskStatus.DONE);

        // Проверяем, что результаты совпадают
        assertEquals(memoryManager.getAllTasks().size(), fileManager.getAllTasks().size(), "Количество задач должно совпадать");
        assertEquals(memoryManager.getAllTasks().get(0).getTitle(), fileManager.getAllTasks().get(0).getTitle(), "Название задачи должно совпадать");
        assertEquals(memoryManager.getAllEpics().size(), fileManager.getAllEpics().size(), "Количество эпиков должно совпадать");
        assertEquals(memoryManager.getAllEpics().get(0).getTitle(), fileManager.getAllEpics().get(0).getTitle(), "Название эпика должно совпадать");
        assertEquals(memoryManager.getAllSubTasks().size(), fileManager.getAllSubTasks().size(), "Количество подзадач должно совпадать");
        assertEquals(memoryManager.getAllSubTasks().get(0).getTitle(), fileManager.getAllSubTasks().get(0).getTitle(), "Название подзадачи должно совпадать");
    }

    /*
    Тест для проверки обновления статуса эпика
     */
    @Test
    void behavesLikeInMemoryTaskManagerForEpicStatusUpdate() {
        FileBackedTaskManager fileManager = new FileBackedTaskManager(tempFile);
        InMemoryTaskManager memoryManager = new InMemoryTaskManager();

        // Создаем эпик и подзадачи
        fileManager.addEpic("Эпик 1", "Описание эпика");
        memoryManager.addEpic("Эпик 1", "Описание эпика");
        fileManager.addSubTask("Подзадача 1", "Описание", 1, TaskStatus.DONE);
        memoryManager.addSubTask("Подзадача 1", "Описание", 1, TaskStatus.DONE);
        fileManager.addSubTask("Подзадача 2", "Описание", 1, TaskStatus.DONE);
        memoryManager.addSubTask("Подзадача 2", "Описание", 1, TaskStatus.DONE);

        // Проверяем статус эпика
        assertEquals(TaskStatus.DONE, fileManager.getAllEpics().get(0).getStatus(), "Статус эпика должен быть DONE в FileBackedTaskManager");
        assertEquals(TaskStatus.DONE, memoryManager.getAllEpics().get(0).getStatus(), "Статус эпика должен быть DONE в InMemoryTaskManager");

        // Обновляем одну подзадачу на IN_PROGRESS
        fileManager.updateSubTask(new SubTask("Подзадача 1", "Описание", 2, TaskStatus.IN_PROGRESS, 1));
        memoryManager.updateSubTask(new SubTask("Подзадача 1", "Описание", 2, TaskStatus.IN_PROGRESS, 1));

        // Проверяем, что статус эпика изменился
        assertEquals(TaskStatus.IN_PROGRESS, fileManager.getAllEpics().get(0).getStatus(), "Статус эпика должен быть IN_PROGRESS в FileBackedTaskManager");
        assertEquals(TaskStatus.IN_PROGRESS, memoryManager.getAllEpics().get(0).getStatus(), "Статус эпика должен быть IN_PROGRESS в InMemoryTaskManager");
    }

    /*
    Тест для проверки удаления задач и эпиков
     */
    @Test
    void behavesLikeInMemoryTaskManagerForDeletion() {
        FileBackedTaskManager fileManager = new FileBackedTaskManager(tempFile);
        InMemoryTaskManager memoryManager = new InMemoryTaskManager();

        // Создаем задачи и эпики
        fileManager.createTask("Задача 1", "Описание", TaskStatus.NEW);
        memoryManager.createTask("Задача 1", "Описание", TaskStatus.NEW);
        fileManager.addEpic("Эпик 1", "Описание эпика");
        memoryManager.addEpic("Эпик 1", "Описание эпика");
        fileManager.addSubTask("Подзадача 1", "Описание подзадачи", 2, TaskStatus.NEW);
        memoryManager.addSubTask("Подзадача 1", "Описание подзадачи", 2, TaskStatus.NEW);

        // Удаляем задачу и эпик
        fileManager.deleteTaskById(1);
        memoryManager.deleteTaskById(1);
        fileManager.deleteEpic(2);
        memoryManager.deleteEpic(2);

        // Проверяем, что задачи и подзадачи удалены
        assertEquals(0, fileManager.getAllTasks().size(), "Список задач должен быть пустым в FileBackedTaskManager");
        assertEquals(0, memoryManager.getAllTasks().size(), "Список задач должен быть пустым в InMemoryTaskManager");
        assertEquals(0, fileManager.getAllEpics().size(), "Список эпиков должен быть пустым в FileBackedTaskManager");
        assertEquals(0, memoryManager.getAllEpics().size(), "Список эпиков должен быть пустым в InMemoryTaskManager");
        assertEquals(0, fileManager.getAllSubTasks().size(), "Список подзадач должен быть пустым в FileBackedTaskManager");
        assertEquals(0, memoryManager.getAllSubTasks().size(), "Список подзадач должен быть пустым в InMemoryTaskManager");
    }
}
