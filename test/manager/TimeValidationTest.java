package manager;

import main.java.main.manager.InMemoryTaskManager;
import main.java.main.model.Epic;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TimeValidationTest {
    private InMemoryTaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void taskWithoutTimeNotInPrioritizedList() {
        Task taskWithTime = manager.createTask("С временем", "Описание", TaskStatus.NEW,
                Duration.ofHours(1), LocalDateTime.now());
        Task taskWithoutTime = manager.createTask("Без времени", "Описание", TaskStatus.NEW);

        List<Task> prioritized = manager.getPrioritizedTasks();

        assertEquals(1, prioritized.size(), "В приоритетном списке должна быть только задача с временем");
        assertEquals(taskWithTime.getId(), prioritized.get(0).getId(), "В списке должна быть задача с временем");
    }

    @Test
    void epicFieldsCalculatedCorrectlyWithMixedSubTasks() {
        manager.addEpic("Эпик", "Описание");
        int epicId = manager.getAllEpics().get(0).getId();

        LocalDateTime start = LocalDateTime.now();

        // Добавляем подзадачи: одну с временем, одну без
        manager.addSubTask("С временем", "Описание", epicId, TaskStatus.NEW,
                Duration.ofHours(2), start);
        manager.addSubTask("Без времени", "Описание", epicId, TaskStatus.NEW);

        Epic epic = manager.getEpicById(epicId);

        assertEquals(Duration.ofHours(2), epic.getDuration(), "Продолжительность эпика должна учитывать только подзадачи с временем");
        assertEquals(start, epic.getStartTime(), "Время начала должно быть от подзадачи с временем");
        assertEquals(start.plusHours(2), epic.getEndTime(), "Время окончания должно быть рассчитано корректно");
    }

    @Test
    void epicWithNoSubTasksHasZeroDurationAndNullTimes() {
        manager.addEpic("Пустой эпик", "Описание");
        Epic epic = manager.getAllEpics().get(0);

        assertEquals(Duration.ZERO, epic.getDuration(), "Продолжительность пустого эпика должна быть 0");
        assertNull(epic.getStartTime(), "Время начала пустого эпика должно быть null");
        assertNull(epic.getEndTime(), "Время окончания пустого эпика должно быть null");
    }

    @Test
    void overlapDetectionWorksWithStreamAPI() {
        LocalDateTime start = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        // Создаем несколько задач
        manager.createTask("Задача 1", "Описание", TaskStatus.NEW, duration, start);
        manager.createTask("Задача 2", "Описание", TaskStatus.NEW, duration, start.plusHours(2));
        manager.addEpic("Эпик", "Описание");
        int epicId = manager.getAllEpics().get(0).getId();
        manager.addSubTask("Подзадача", "Описание", epicId, TaskStatus.NEW, duration, start.plusHours(4));

        // Проверяем пересечение с существующей задачей
        Task overlappingTask1 = new Task("Пересечение 1", "Описание", 999, TaskStatus.NEW,
                duration, start.plusMinutes(30));
        assertTrue(manager.hasOverlapWithExistingTasks(overlappingTask1), "Должно найти пересечение с первой задачей");

        // Проверяем пересечение с подзадачей
        Task overlappingTask2 = new Task("Пересечение 2", "Описание", 998, TaskStatus.NEW,
                duration, start.plusHours(4).plusMinutes(30));
        assertTrue(manager.hasOverlapWithExistingTasks(overlappingTask2), "Должно найти пересечение с подзадачей");

        // Проверяем отсутствие пересечения
        Task nonOverlappingTask = new Task("Без пересечения", "Описание", 997, TaskStatus.NEW,
                duration, start.plusHours(6));
        assertFalse(manager.hasOverlapWithExistingTasks(nonOverlappingTask), "Не должно найти пересечение");
    }

    @Test
    void borderCaseOverlapDetection() {
        LocalDateTime start = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        manager.createTask("Базовая задача", "Описание", TaskStatus.NEW, duration, start);

        // Задача, которая начинается точно когда заканчивается первая - НЕ должна пересекаться
        Task adjacentTask = new Task("Смежная задача", "Описание", 999, TaskStatus.NEW,
                duration, start.plusHours(1));
        assertFalse(manager.hasOverlapWithExistingTasks(adjacentTask),
                "Смежные задачи не должны считаться пересекающимися");

        // Задача, которая начинается на минуту раньше окончания первой - должна пересекаться
        Task overlappingTask = new Task("Пересекающаяся задача", "Описание", 998, TaskStatus.NEW,
                duration, start.plusMinutes(59));
        assertTrue(manager.hasOverlapWithExistingTasks(overlappingTask),
                "Задачи с частичным пересечением должны считаться пересекающимися");
    }

    @Test
    void updateTaskMovesItInPrioritizedList() {
        LocalDateTime start = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        Task task1 = manager.createTask("Задача 1", "Описание", TaskStatus.NEW, duration, start.plusHours(2));
        Task task2 = manager.createTask("Задача 2", "Описание", TaskStatus.NEW, duration, start.plusHours(1));

        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(start.plusHours(1), prioritized.get(0).getStartTime(), "Первой должна быть задача 2");
        assertEquals(start.plusHours(2), prioritized.get(1).getStartTime(), "Второй должна быть задача 1");

        // Обновляем время первой задачи, делая её самой ранней
        task1.setStartTime(start);
        manager.updateTask(task1);

        prioritized = manager.getPrioritizedTasks();
        assertEquals(start, prioritized.get(0).getStartTime(), "После обновления первой должна стать задача 1");
        assertEquals(start.plusHours(1), prioritized.get(1).getStartTime(), "Второй должна остаться задача 2");
    }

    @Test
    void deleteTaskRemovesFromPrioritizedList() {
        LocalDateTime start = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        Task task1 = manager.createTask("Задача 1", "Описание", TaskStatus.NEW, duration, start);
        Task task2 = manager.createTask("Задача 2", "Описание", TaskStatus.NEW, duration, start.plusHours(1));

        assertEquals(2, manager.getPrioritizedTasks().size(), "В списке должно быть 2 задачи");

        manager.deleteTaskById(task1.getId());

        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(1, prioritized.size(), "После удаления в списке должна остаться 1 задача");
        assertEquals(task2.getId(), prioritized.get(0).getId(), "Должна остаться вторая задача");
    }

    @Test
    void subTaskOverlapValidationWorksCorrectly() {
        LocalDateTime start = LocalDateTime.now();
        Duration duration = Duration.ofHours(1);

        manager.createTask("Существующая задача", "Описание", TaskStatus.NEW, duration, start);
        manager.addEpic("Эпик", "Описание");
        int epicId = manager.getAllEpics().get(0).getId();

        // Попытка добавить пересекающуюся подзадачу должна вызвать исключение
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addSubTask("Пересекающаяся подзадача", "Описание", epicId, TaskStatus.NEW,
                    duration, start.plusMinutes(30));
        }, "Должно выбросить исключение при добавлении пересекающейся подзадачи");
    }
}