package test.java.main.manager;

import main.java.main.manager.InMemoryHistoryManager;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

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
        List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size(), "История должна содержать 2 задачи");
        assertEquals(originalTask1, history.get(0), "История должна сохранить исходное состояние задачи");
        assertEquals(task2, history.get(1), "История должна содержать вторую задачу");
    }

    @Test
    void removesDuplicatesFromHistory() {
        //Добавляем задачу несколько раз
        Task task = new Task("Задача", "Описание", 1, TaskStatus.NEW);
        historyManager.add(task);
        task.setStatus(TaskStatus.IN_PROGRESS);
        historyManager.add(task);
        task.setStatus(TaskStatus.DONE);
        historyManager.add(task);

        //Проверяем, что в истории находится только последний просмотр
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "История должна содержать только один просмотр задачи");
        assertEquals(TaskStatus.DONE, history.get(0).getStatus(), "История должна содержать последний статус задачи");
    }

    @Test
    void removesTaskFromHistory() {
        //Добавляем задачи в историю
        Task task1 = new Task("Задача 1", "Описание 1", 1, TaskStatus.NEW);
        Task task2 = new Task("Задача 2", "Описание 2", 2, TaskStatus.NEW);
        historyManager.add(task1);
        historyManager.add(task2);

        //Удаляем первую задачу
        historyManager.remove(1);

        //Проверяем что задача удалена из истории
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "История должна содержать только одну задачу после удаления");
        assertEquals(task2, history.get(0), "Вторая задача должна остаться");
    }


}