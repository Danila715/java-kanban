package test.manager;

import main.model.*;
import main.manager.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {
    private InMemoryTaskManager manager;

    @BeforeEach
    void setUp() {
        // Инициализируем менеджер перед каждым тестом
        manager = new InMemoryTaskManager();
    }

    @Test
    void addsAndRetrievesTasksById() {
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
    void tasksWithSpecifiedAndGeneratedIdsDoNotConflict() {
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
    void taskSavedStateWhenAdded() {
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
    void epicCannotBeItsOwnSubTask() {
        // Создаем эпик
        Epic epic = new Epic("Эпик", "Описание", 1);
        manager.addEpic(epic.getTitle(), epic.getDescription());
        // Пытаемся добавить эпик как подзадачу самого себя
        manager.addSubTask("Подзадача", "Описание", epic.getId(), TaskStatus.NEW);

        // Проверяем, что эпик не добавлен в список своих подзадач
        assertFalse(epic.getSubTaskIds().contains(epic.getId()), "Эпик не должен быть добавлен как собственная подзадача");
    }

    @Test
    void subTaskCannotBeItsOwnEpic() {
        // Создаем эпик и подзадачу
        Epic epic = new Epic("Эпик", "Описание", 1);
        manager.addEpic(epic.getTitle(), epic.getDescription());
        SubTask subTask = new SubTask("Подзадача", "Описание", 2, TaskStatus.NEW, epic.getId());
        manager.addSubTask(subTask.getTitle(), subTask.getDescription(), epic.getId(), subTask.getStatus());

        // Проверяем, что подзадача не является своим собственным эпиком
        assertNotEquals(subTask.getId(), subTask.getEpicId(), "Подзадача не может быть своим собственным эпиком");
    }

    @Test
    void epicStatusIsDoneWhenAllSubTasksAreDone() {
        // Создаем эпик и добавляем две подзадачи со статусом DONE
        int epicId = 1;
        Epic epic = new Epic("Эпик", "Описание", epicId);
        manager.addEpic(epic.getTitle(), epic.getDescription());
        manager.addSubTask("Подзадача 1", "Описание", epicId, TaskStatus.DONE);
        manager.addSubTask("Подзадача 2", "Описание", epicId, TaskStatus.DONE);

        // Проверяем, что статус эпика становится DONE
        assertEquals(TaskStatus.DONE, manager.getEpicById(epicId).getStatus(), "Эпик должен иметь статус DONE, если все подзадачи имеют статус DONE");
    }
}