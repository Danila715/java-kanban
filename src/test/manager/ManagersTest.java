package test.manager;

import main.manager.*;
import main.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ManagersTest {
    @Test
    void getDefaultReturnsInitializedTaskManager() {
        // Получаем менеджер задач
        TaskManager taskManager = Managers.getDefault();
        // Проверяем, что менеджер задач не null
        assertNotNull(taskManager, "Менеджер задач не должен быть null");

        // Проверяем, что менеджер может добавлять задачи
        taskManager.createTask("Тестовая задача", "Описание", TaskStatus.NEW);
        assertFalse(taskManager.getAllTasks().isEmpty(), "Менеджер задач должен уметь добавлять задачи");
    }

    @Test
    void getDefaultHistoryReturnsInitializedHistoryManager() {
        // Получаем менеджер истории
        HistoryManager historyManager = Managers.getDefaultHistory();
        // Проверяем, что менеджер истории не null
        assertNotNull(historyManager, "Менеджер истории не должен быть null");

        // Проверяем, что менеджер истории может добавлять задачи
        Task task = new Task("Тестовая задача", "Описание", 1, TaskStatus.NEW);
        historyManager.add(task);
        assertFalse(historyManager.getHistory().isEmpty(), "Менеджер истории должен уметь добавлять задачи в историю");
    }
}