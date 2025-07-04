package test.manager;

import main.manager.*;
import main.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ManagersTest {
    @Test
    void getDefaultReturnsInitializedTaskManager() {
        TaskManager taskManager = Managers.getDefault();
        assertNotNull(taskManager, "Менеджер задач не должен быть null");
        assertInstanceOf(InMemoryTaskManager.class, taskManager,
                "Менеджер задач должен быть экземпляром InMemoryTaskManager");

        taskManager.createTask("Тестовая задача", "Описание", TaskStatus.NEW);
        assertFalse(taskManager.getAllTasks().isEmpty(), "Менеджер задач должен уметь добавлять задачи");
    }

    @Test
    void getDefaultHistoryReturnsInitializedHistoryManager() {
        HistoryManager historyManager = Managers.getDefaultHistory();
        assertNotNull(historyManager, "Менеджер истории не должен быть null");
        assertInstanceOf(InMemoryHistoryManager.class, historyManager,
                "Менеджер истории должен быть экземпляром InMemoryHistoryManager");

        Task task = new Task("Тестовая задача", "Описание", 1, TaskStatus.NEW);
        historyManager.add(task);
        assertFalse(historyManager.getHistory().isEmpty(), "Менеджер истории должен уметь добавлять задачи в историю");
    }
}