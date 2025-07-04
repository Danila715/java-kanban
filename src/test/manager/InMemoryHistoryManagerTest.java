package test.manager;

import main.manager.*;
import main.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

public class InMemoryHistoryManagerTest {
    private InMemoryHistoryManager historyManager;

    /*Инициализируем менеджер истории перед каждым тестом*/
    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
    }

    @Test
    void addsTasksToHistoryAndSaveState() {
        // Создаем две задачи и добавляем их в историю
        Task task1 = new Task("Задача 1", "Описание 1", 1, TaskStatus.NEW);
        Task task2 = new Task("Задача 2", "Описание 2", 2, TaskStatus.IN_PROGRESS);
        historyManager.add(task1);
        // Сохраняем копию первой задачи
        Task originalTask1 = new Task(task1.getTitle(), task1.getDescription(), task1.getId(), task1.getStatus());
        // Изменяем статус первой задачи
        task1.setStatus(TaskStatus.DONE);
        historyManager.add(task2);

        // Проверяем, что история содержит задачи в правильном порядке и с сохраненным состоянием
        ArrayList<Task> history = historyManager.getHistory();
        assertEquals(2, history.size(), "История должна содержать 2 задачи");
        assertEquals(originalTask1, history.get(0), "История должна сохранить исходное состояние задачи");
        assertEquals(task2, history.get(1), "История должна содержать вторую задачу");
    }

    @Test
    void limitsHistoryToTenTasks() {
        // Добавляем 12 задач в историю
        for (int i = 1; i <= 12; i++) {
            Task task = new Task("Задача " + i, "Описание " + i, i, TaskStatus.NEW);
            historyManager.add(task);
        }

        // Проверяем, что история ограничена 10 задачами
        ArrayList<Task> history = historyManager.getHistory();
        assertEquals(10, history.size(), "История должна быть ограничена 10 задачами");
        assertEquals(3, history.get(0).getId(), "Самая старая задача должна иметь ID 3");
        assertEquals(12, history.get(9).getId(), "Самая новая задача должна иметь ID 12");
    }
}