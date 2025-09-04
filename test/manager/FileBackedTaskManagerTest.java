package manager;

import main.java.main.manager.FileBackedTaskManager;
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
    private FileBackedTaskManager manager;

    /*
    Создаем временный файл перед каждым тестом
     */
    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("tasks", ".csv");
        manager = FileBackedTaskManager.loadFromFile(tempFile);
    }

    /*
    Тест для сохранения и загрузки пустого файла
     */
    @Test
    void saveAndLoadEmptyFile() {
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
    void saveMultipleTasks() throws IOException {
        manager.createTask("Задача 1", "Описание задачи 1", TaskStatus.NEW);
        manager.addEpic("Эпик 1", "Описание эпика 1");
        manager.addSubTask("Подзадача 1", "Описание подзадачи 1", 2, TaskStatus.DONE);

        // Проверяем содержимое файла
        BufferedReader reader = new BufferedReader(new FileReader(tempFile));
        String header = reader.readLine();
        String taskLine = reader.readLine();
        String epicLine = reader.readLine();
        String subTaskLine = reader.readLine();
        reader.close();

        assertEquals("id,type,name,status,description,epic", header, "Заголовок CSV должен быть корректным");
        assertEquals("1,TASK,Задача 1,NEW,Описание задачи 1,", taskLine, "Задача должна быть сохранена в CSV");
        assertEquals("2,EPIC,Эпик 1,DONE,Описание эпика 1,", epicLine, "Эпик должен быть сохранен в CSV");
        assertEquals("3,SUBTASK,Подзадача 1,DONE,Описание подзадачи 1,2", subTaskLine, "Подзадача должна быть сохранена в CSV");
    }

    /*
    Тест для загрузки нескольких задач, эпиков и подзадач
     */
    @Test
    void loadMultipleTasks() {
        manager.createTask("Задача 1", "Описание задачи 1", TaskStatus.NEW);
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
    Тест для проверки обновления статуса эпика в FileBackedTaskManager
     */
    @Test
    void testEpicStatusUpdate() {
        manager.addEpic("Эпик 1", "Описание эпика");
        manager.addSubTask("Подзадача 1", "Описание", 1, TaskStatus.DONE);
        manager.addSubTask("Подзадача 2", "Описание", 1, TaskStatus.DONE);

        // Проверяем, что статус эпика стал DONE
        Epic epic = manager.getAllEpics().get(0);
        assertEquals(TaskStatus.DONE, epic.getStatus(), "Статус эпика должен быть DONE, если все подзадачи DONE");

        // Обновляем подзадачу на IN_PROGRESS
        manager.updateSubTask(new SubTask("Подзадача 1", "Описание", 2, TaskStatus.IN_PROGRESS, 1));

        // Проверяем, что статус эпика стал IN_PROGRESS
        assertEquals(TaskStatus.IN_PROGRESS, epic.getStatus(), "Статус эпика должен быть IN_PROGRESS при разных статусах подзадач");
    }

    /*
    Тест для проверки удаления задач и эпиков в FileBackedTaskManager
     */
    @Test
    void testTaskAndEpicDeletion() {
        // Создаем задачу, эпик и подзадачу
        manager.createTask("Задача 1", "Описание", TaskStatus.NEW);
        manager.addEpic("Эпик 1", "Описание эпика");
        manager.addSubTask("Подзадача 1", "Описание подзадачи", 2, TaskStatus.NEW);

        // Удаляем задачу
        manager.deleteTaskById(1);
        assertEquals(0, manager.getAllTasks().size(), "Список задач должен быть пустым после удаления");

        // Удаляем эпик (должна удалиться и подзадача)
        manager.deleteEpic(2);
        assertEquals(0, manager.getAllEpics().size(), "Список эпиков должен быть пустым после удаления");
        assertEquals(0, manager.getAllSubTasks().size(), "Список подзадач должен быть пустым после удаления эпика");
    }
}
