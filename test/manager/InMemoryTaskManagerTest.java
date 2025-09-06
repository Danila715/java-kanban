package manager;

import main.java.main.manager.InMemoryTaskManager;
import main.java.main.manager.TaskOverlapException;
import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {
    private InMemoryTaskManager manager;

    @BeforeEach
    void setUp() {
        // Инициализируем менеджер перед каждым тестом
        manager = new InMemoryTaskManager();
    }

    @Test
    void addsAndRetrievesTasksById() throws TaskOverlapException {
        // Создаем задачу, эпик и подзадачу
        Task task = manager.createTask("Задача", "Описание", TaskStatus.NEW);
        int taskId = task.getId();
        manager.addEpic("Эпик", "Описание");
        Epic epic = manager.getAllEpics().get(0);
        int epicId = epic.getId();
        manager.addSubTask("Подзадача", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask = manager.getAllSubTasks().get(0);
        int subTaskId = subTask.getId();

        // Проверяем, что задачи можно получить по ID
        assertEquals(task, manager.getTaskById(taskId), "Должна быть получена задача по ID");
        assertEquals(epic, manager.getEpicById(epicId), "Должен быть получен эпик по ID");
        assertEquals(subTask, manager.getSubTaskById(subTaskId), "Должна быть получена подзадача по ID");
    }

    @Test
    void tasksWithSpecifiedAndGeneratedIdsDoNotConflict() throws TaskOverlapException {
        // Создаем задачу с автоматически сгенерированным ID
        Task task1 = manager.createTask("Задача 1", "Описание 1", TaskStatus.NEW);
        int generatedId = task1.getId();
        // Создаем задачу с явно заданным ID
        Task task2 = manager.createTaskWithId("Задача 2", "Описание 2", generatedId + 1, TaskStatus.NEW);

        // Проверяем, что задачи с разными ID не конфликтуют
        assertEquals(task1, manager.getTaskById(generatedId), "Должна быть получена задача с сгенерированным ID");
        assertEquals(task2, manager.getTaskById(generatedId + 1), "Должна быть получена задача с заданным ID");
        assertNotEquals(task1, task2, "Задачи с разными ID не должны быть равны");
    }

    @Test
    void taskSavedStateWhenAdded() throws TaskOverlapException {
        // Создаем задачу и сохраняем ее копию
        int id = 1;
        Task task = manager.createTaskWithId("Задача", "Описание", id, TaskStatus.NEW);
        Task original = new Task(task.getTitle(), task.getDescription(), task.getId(), task.getStatus());

        // Проверяем, что поля задачи не изменились после добавления
        Task retrieved = manager.getTaskById(id);
        assertEquals(original.getTitle(), retrieved.getTitle(), "Название задачи должно остаться неизменным");
        assertEquals(original.getDescription(), retrieved.getDescription(), "Описание задачи должно остаться неизменным");
        assertEquals(original.getId(), retrieved.getId(), "ID задачи должен остаться неизменным");
        assertEquals(original.getStatus(), retrieved.getStatus(), "Статус задачи должен остаться неизменным");
    }

    @Test
    void epicCannotBeItsOwnSubTask() throws TaskOverlapException {
        // Создаем эпик
        Epic epic = new Epic("Эпик", "Описание", 1);
        manager.addEpic(epic.getTitle(), epic.getDescription());
        // Пытаемся добавить эпик как подзадачу самого себя
        manager.addSubTask("Подзадача", "Описание", epic.getId(), TaskStatus.NEW);

        // Проверяем, что эпик не добавлен в список своих подзадач
        assertFalse(epic.getSubTaskIds().contains(epic.getId()), "Эпик не должен быть добавлен как собственная подзадача");
    }

    @Test
    void subTaskCannotBeItsOwnEpic() throws TaskOverlapException {
        // Создаем эпик и подзадачу
        Epic epic = new Epic("Эпик", "Описание", 1);
        manager.addEpic(epic.getTitle(), epic.getDescription());
        SubTask subTask = new SubTask("Подзадача", "Описание", 2, TaskStatus.NEW, epic.getId());
        manager.addSubTask(subTask.getTitle(), subTask.getDescription(), epic.getId(), subTask.getStatus());

        // Проверяем, что подзадача не является своим собственным эпиком
        assertNotEquals(subTask.getId(), subTask.getEpicId(), "Подзадача не может быть своим собственным эпиком");
    }

    @Test
    void epicStatusIsDoneWhenAllSubTasksAreDone() throws TaskOverlapException {
        // Создаем эпик и добавляем две подзадачи со статусом DONE
        int epicId = 1;
        Epic epic = new Epic("Эпик", "Описание", epicId);
        manager.addEpic(epic.getTitle(), epic.getDescription());
        manager.addSubTask("Подзадача 1", "Описание", epicId, TaskStatus.DONE);
        manager.addSubTask("Подзадача 2", "Описание", epicId, TaskStatus.DONE);

        // Проверяем, что статус эпика становится DONE
        assertEquals(TaskStatus.DONE, manager.getEpicById(epicId).getStatus(), "Эпик должен иметь статус DONE, если все подзадачи имеют статус DONE");
    }

    @Test
    void deletedSubTasksDoNotKeepOldIds() throws TaskOverlapException {
        // Создаем эпик и подзадачу
        int epicId = 1;
        manager.addEpic("Эпик", "Описание");
        manager.addSubTask("Подзадача", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask = manager.getAllSubTasks().get(0);
        int subTaskId = subTask.getId();

        // Удаляем подзадачу
        manager.deleteSubTask(subTaskId);

        // Проверяем, что подзадача удалена из менеджера и истории
        assertNull(manager.getSubTaskById(subTaskId), "Подзадача должна быть удалена из менеджера");
        assertFalse(manager.getHistory().stream().anyMatch(task -> task.getId() == subTaskId), "Подзадача не должна оставаться в истории");
    }

    @Test
    void epicsDoNotKeepDeletedSubTaskIds() throws TaskOverlapException {
        // Создаем эпик и две подзадачи
        int epicId = 1;
        manager.addEpic("Эпик", "Описание");
        manager.addSubTask("Подзадача 1", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask1 = manager.getAllSubTasks().get(0);
        int subTaskId1 = subTask1.getId();
        manager.addSubTask("Подзадача 2", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask2 = manager.getAllSubTasks().get(1);
        int subTaskId2 = subTask2.getId();

        // Удаляем одну подзадачу
        manager.deleteSubTask(subTaskId1);

        // Проверяем, что эпик не содержит ID удаленной подзадачи
        Epic epic = manager.getEpicById(epicId);
        assertFalse(epic.getSubTaskIds().contains(subTaskId1), "Эпик не должен содержать ID удаленной подзадачи");
        assertTrue(epic.getSubTaskIds().contains(subTaskId2), "Эпик должен содержать ID оставшейся подзадачи");
    }

    @Test
    void settersDoNotAffectManagerData() throws TaskOverlapException {
        // Создаем задачу и подзадачу
        Task task = manager.createTaskWithId("Задача", "Описание", 1, TaskStatus.NEW);
        int epicId = 2;
        manager.addEpic("Эпик", "Описание");
        manager.addSubTask("Подзадача", "Описание", epicId, TaskStatus.NEW);
        SubTask subTask = manager.getAllSubTasks().get(0);
        int subTaskId = subTask.getId();

        // Изменяем поля через сеттеры
        task.setTitle("Новое название");
        task.setStatus(TaskStatus.DONE);
        subTask.setTitle("Новое название подзадачи");
        subTask.setStatus(TaskStatus.DONE);
        subTask.setEpicId(999);

        // Проверяем, что данные в менеджере не изменились
        Task storedTask = manager.getTaskById(1);
        assertEquals("Задача", storedTask.getTitle(), "Название задачи в менеджере не должно измениться");
        assertEquals(TaskStatus.NEW, storedTask.getStatus(), "Статус задачи в менеджере не должен измениться");

        SubTask storedSubTask = manager.getSubTaskById(subTaskId);
        assertEquals("Подзадача", storedSubTask.getTitle(), "Название подзадачи в менеджере не должно измениться");
        assertEquals(TaskStatus.NEW, storedSubTask.getStatus(), "Статус подзадачи в менеджере не должен измениться");
        assertEquals(epicId, storedSubTask.getEpicId(), "ID подзадачи эпика в менеджере не должен измениться");
    }

    /*
    Тесты для временных полей и приоритетов
     */

    @Test
    void taskWithDurationAndStartTime() throws TaskOverlapException {
        Duration duration = Duration.ofHours(2);
        LocalDateTime startTime = LocalDateTime.now();

        Task task = manager.createTask("Задача с временем", "Описание", TaskStatus.NEW, duration, startTime);

        assertEquals(duration, task.getDuration(), "Продолжительность должна быть установлена корректно");
        assertEquals(startTime, task.getStartTime(), "Время начала должно быть установлено корректно");
        assertEquals(startTime.plus(duration), task.getEndTime(), "Время окончания должно быть рассчитано корректно");
    }

    @Test
    void epicCalculatesFieldsFromSubTasks() throws TaskOverlapException {
        manager.addEpic("Эпик с временем", "Описание");
        Epic epic = manager.getAllEpics().get(0);
        int epicId = epic.getId();

        LocalDateTime start1 = LocalDateTime.now();
        LocalDateTime start2 = start1.plusHours(3);
        Duration duration1 = Duration.ofHours(2);
        Duration duration2 = Duration.ofHours(1);

        manager.addSubTask("Подзадача 1", "Описание", epicId, TaskStatus.NEW, duration1, start1);
        manager.addSubTask("Подзадача 2", "Описание", epicId, TaskStatus.NEW, duration2, start2);

        Epic updatedEpic = manager.getEpicById(epicId);

        assertEquals(duration1.plus(duration2), updatedEpic.getDuration(), "Продолжительность эпика должна быть суммой подзадач");
        assertEquals(start1, updatedEpic.getStartTime(), "Время начала эпика должно быть самым ранним из подзадач");
        assertEquals(start2.plus(duration2), updatedEpic.getEndTime(), "Время окончания эпика должно быть самым поздним из подзадач");
    }

    @Test
    void getPrioritizedTasksReturnsSortedByStartTime() throws TaskOverlapException {
        LocalDateTime now = LocalDateTime.now();

        manager.createTask("Задача 3", "Описание", TaskStatus.NEW, Duration.ofHours(1), now.plusHours(2));
        manager.createTask("Задача 1", "Описание", TaskStatus.NEW, Duration.ofHours(1), now);
        manager.createTask("Задача 2", "Описание", TaskStatus.NEW, Duration.ofHours(1), now.plusHours(1));
        manager.createTask("Задача без времени", "Описание", TaskStatus.NEW); // без времени - не попадет в список

        List<Task> prioritized = manager.getPrioritizedTasks();

        assertEquals(3, prioritized.size(), "В приоритетном списке должно быть 3 задачи (без учета задачи без времени)");
        assertEquals(now, prioritized.get(0).getStartTime(), "Первая задача должна быть самой ранней");
        assertEquals(now.plusHours(1), prioritized.get(1).getStartTime(), "Вторая задача должна быть средней");
        assertEquals(now.plusHours(2), prioritized.get(2).getStartTime(), "Третья задача должна быть самой поздней");
    }

    @Test
    void checkTaskOverlapReturnsTrueForOverlappingTasks() {
        LocalDateTime start1 = LocalDateTime.now();
        LocalDateTime start2 = start1.plusMinutes(30);
        Duration duration = Duration.ofHours(1);

        Task task1 = new Task("Задача 1", "Описание", 1, TaskStatus.NEW, duration, start1);
        Task task2 = new Task("Задача 2", "Описание", 2, TaskStatus.NEW, duration, start2);

        assertTrue(manager.checkTaskOverlap(task1, task2), "Задачи должны пересекаться по времени");
    }

    @Test
    void checkTaskOverlapReturnsFalseForNonOverlappingTasks() {
        LocalDateTime start1 = LocalDateTime.now();
        LocalDateTime start2 = start1.plusHours(2);
        Duration duration = Duration.ofHours(1);

        Task task1 = new Task("Задача 1", "Описание", 1, TaskStatus.NEW, duration, start1);
        Task task2 = new Task("Задача 2", "Описание", 2, TaskStatus.NEW, duration, start2);

        assertFalse(manager.checkTaskOverlap(task1, task2), "Задачи не должны пересекаться по времени");
    }

    @Test
    void hasOverlapWithExistingTasksReturnsTrueWhenOverlapExists() throws TaskOverlapException {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        manager.createTask("Существующая задача", "Описание", TaskStatus.NEW, duration, now);

        Task overlappingTask = new Task("Пересекающаяся задача", "Описание", 999, TaskStatus.NEW,
                duration, now.plusMinutes(30));

        assertTrue(manager.hasOverlapWithExistingTasks(overlappingTask), "Должно определить пересечение с существующей задачей");
    }

    @Test
    void createTaskThrowsExceptionWhenOverlapDetected() throws TaskOverlapException {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        manager.createTask("Существующая задача", "Описание", TaskStatus.NEW, duration, now);

        assertThrows(TaskOverlapException.class, () -> {
            manager.createTask("Пересекающаяся задача", "Описание", TaskStatus.NEW, duration, now.plusMinutes(30));
        }, "Должно выбросить исключение при попытке создать пересекающуюся задачу");
    }


    @Test
    void updateTaskThrowsExceptionWhenOverlapDetected() throws TaskOverlapException {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        Task task1 = manager.createTask("Задача 1", "Описание", TaskStatus.NEW, duration, now);
        manager.createTask("Задача 2", "Описание", TaskStatus.NEW, duration, now.plusHours(2));

        task1.setStartTime(now.plusHours(2).plusMinutes(30));

        assertThrows(TaskOverlapException.class, () -> {
            manager.updateTask(task1);
        }, "Должно выбросить исключение при попытке обновить задачу с пересечением");
    }
}