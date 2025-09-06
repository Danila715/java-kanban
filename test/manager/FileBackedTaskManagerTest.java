package manager;

import main.java.main.manager.FileBackedTaskManager;
import main.java.main.manager.Managers;
import main.java.main.manager.TaskOverlapException;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileBackedTaskManagerTest {
    private File tempFile;
    private FileBackedTaskManager manager;

    /*
    Создаем временный файл перед каждым тестом
     */
    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("tasks", ".csv");
        manager = Managers.getDefaultFileBacked(tempFile);
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
    void saveMultipleTasks() throws IOException, TaskOverlapException {
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 10, 0);
        Duration duration = Duration.ofHours(2);

        manager.createTask("Задача 1", "Описание задачи 1", TaskStatus.NEW, duration, startTime);
        manager.addEpic("Эпик 1", "Описание эпика 1");
        manager.addSubTask("Подзадача 1", "Описание подзадачи 1", 2, TaskStatus.DONE,
                Duration.ofHours(1), startTime.plusHours(3));

        // Проверяем содержимое файла
        BufferedReader reader = new BufferedReader(new FileReader(tempFile));
        String header = reader.readLine();
        String taskLine = reader.readLine();
        String epicLine = reader.readLine();
        String subTaskLine = reader.readLine();
        reader.close();

        assertEquals("id,type,name,status,description,epic,duration,startTime", header, "Заголовок CSV должен быть корректным");
        assertEquals("1,TASK,Задача 1,NEW,Описание задачи 1,,120,2025-01-15 10:00", taskLine, "Задача должна быть сохранена в CSV с временными полями");
        assertEquals("2,EPIC,Эпик 1,DONE,Описание эпика 1,,60,2025-01-15 13:00", epicLine, "Эпик должен быть сохранен в CSV");
        assertEquals("3,SUBTASK,Подзадача 1,DONE,Описание подзадачи 1,2,60,2025-01-15 13:00", subTaskLine, "Подзадача должна быть сохранена в CSV с временными полями");
    }

    /*
    Тест для загрузки нескольких задач, эпиков и подзадач с временными полями
     */
    @Test
    void loadMultipleTasksWithTimeFields() throws TaskOverlapException {
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 15, 10, 0);
        Duration duration = Duration.ofHours(2);

        manager.createTask("Задача 1", "Описание задачи 1", TaskStatus.NEW, duration, startTime);
        manager.addEpic("Эпик 1", "Описание эпика 1");
        manager.addSubTask("Подзадача 1", "Описание подзадачи 1", 2, TaskStatus.DONE,
                Duration.ofHours(1), startTime.plusHours(3));

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        List<Task> tasks = loadedManager.getAllTasks();
        List<Epic> epics = loadedManager.getAllEpics();
        List<SubTask> subTasks = loadedManager.getAllSubTasks();

        assertEquals(1, tasks.size(), "Должна быть загружена одна задача");
        assertEquals("Задача 1", tasks.get(0).getTitle(), "Название задачи должно быть корректным");
        assertEquals(TaskStatus.NEW, tasks.get(0).getStatus(), "Статус задачи должен быть NEW");
        assertEquals(duration, tasks.get(0).getDuration(), "Продолжительность задачи должна быть загружена корректно");
        assertEquals(startTime, tasks.get(0).getStartTime(), "Время начала задачи должно быть загружено корректно");
        assertEquals(startTime.plus(duration), tasks.get(0).getEndTime(), "Время окончания должно быть рассчитано корректно");

        assertEquals(1, epics.size(), "Должен быть загружен один эпик");
        assertEquals("Эпик 1", epics.get(0).getTitle(), "Название эпика должно быть корректным");

        assertEquals(1, subTasks.size(), "Должна быть загружена одна подзадача");
        assertEquals("Подзадача 1", subTasks.get(0).getTitle(), "Название подзадачи должно быть корректным");
        assertEquals(TaskStatus.DONE, subTasks.get(0).getStatus(), "Статус подзадачи должен быть DONE");
        assertEquals(2, subTasks.get(0).getEpicId(), "ID эпика подзадачи должен быть корректным");
        assertEquals(Duration.ofHours(1), subTasks.get(0).getDuration(), "Продолжительность подзадачи должна быть загружена корректно");
        assertEquals(startTime.plusHours(3), subTasks.get(0).getStartTime(), "Время начала подзадачи должно быть загружено корректно");
    }

    /*
    Тест для проверки обновления статуса эпика в FileBackedTaskManager
     */
    @Test
    void testEpicStatusUpdate() throws TaskOverlapException {
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
    void testTaskAndEpicDeletion() throws TaskOverlapException {
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

    /*
    Тест для проверки расчета полей эпика на основе подзадач
     */
    @Test
    void testEpicFieldsCalculation() throws TaskOverlapException {
        LocalDateTime start1 = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime start2 = LocalDateTime.of(2025, 1, 15, 14, 0);
        Duration duration1 = Duration.ofHours(2);
        Duration duration2 = Duration.ofHours(1);

        manager.addEpic("Эпик с временем", "Описание эпика");
        int epicId = manager.getAllEpics().get(0).getId();

        manager.addSubTask("Подзадача 1", "Описание", epicId, TaskStatus.NEW, duration1, start1);
        manager.addSubTask("Подзадача 2", "Описание", epicId, TaskStatus.NEW, duration2, start2);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        Epic loadedEpic = loadedManager.getAllEpics().get(0);

        assertEquals(duration1.plus(duration2), loadedEpic.getDuration(), "Продолжительность эпика должна быть суммой подзадач");
        assertEquals(start1, loadedEpic.getStartTime(), "Время начала эпика должно быть самым ранним из подзадач");
        assertEquals(start2.plus(duration2), loadedEpic.getEndTime(), "Время окончания эпика должно быть самым поздним из подзадач");
    }

    /*
    Тест для проверки сохранения приоритетного списка задач
     */
    @Test
    void testPrioritizedTasksSaveAndLoad() throws TaskOverlapException {
        LocalDateTime now = LocalDateTime.of(2025, 1, 15, 10, 0);

        manager.createTask("Задача 3", "Описание", TaskStatus.NEW, Duration.ofHours(1), now.plusHours(2));
        manager.createTask("Задача 1", "Описание", TaskStatus.NEW, Duration.ofHours(1), now);
        manager.createTask("Задача 2", "Описание", TaskStatus.NEW, Duration.ofHours(1), now.plusHours(1));
        manager.createTask("Задача без времени", "Описание", TaskStatus.NEW); // без времени - не попадет в список

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        List<Task> prioritized = loadedManager.getPrioritizedTasks();

        assertEquals(3, prioritized.size(), "В приоритетном списке должно быть 3 задачи (без учета задачи без времени)");
        assertEquals(now, prioritized.get(0).getStartTime(), "Первая задача должна быть самой ранней");
        assertEquals(now.plusHours(1), prioritized.get(1).getStartTime(), "Вторая задача должна быть средней");
        assertEquals(now.plusHours(2), prioritized.get(2).getStartTime(), "Третья задача должна быть самой поздней");
    }

    /*
    Тест проверки пересечений после загрузки из файла
     */
    @Test
    void testOverlapValidationAfterLoad() throws TaskOverlapException {
        LocalDateTime now = LocalDateTime.of(2025, 1, 15, 10, 0);
        Duration duration = Duration.ofHours(1);

        manager.createTask("Задача 1", "Описание", TaskStatus.NEW, duration, now);
        manager.createTask("Задача 2", "Описание", TaskStatus.NEW, duration, now.plusHours(2));

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);

        // Попытка добавить пересекающуюся задачу должна вызвать TaskOverlapException
        TaskOverlapException exception = assertThrows(TaskOverlapException.class, () -> {
            loadedManager.createTask("Пересекающаяся задача", "Описание", TaskStatus.NEW,
                    duration, now.plusMinutes(30));
        });

        assertEquals("Задача пересекается по времени с существующими задачами", exception.getMessage());
    }
}