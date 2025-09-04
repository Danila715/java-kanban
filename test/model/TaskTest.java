package model;

import main.java.main.model.Task;
import main.java.main.model.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class TaskTest {
    @Test
    void tasksWithSameIdAreEqual() {
        Task task1 = new Task("Задача 1", "Описание 1", 1, TaskStatus.NEW);
        Task task2 = new Task("Задача 2", "Описание 2", 1, TaskStatus.IN_PROGRESS);
        assertEquals(task1, task2, "Задачи с одинаковым ID должны быть равны");
        assertNotSame(task1, task2, "Задачи не должны быть одним и тем же объектом");
        assertEquals(task1.hashCode(), task2.hashCode(), "Задачи с одинаковым ID должны иметь одинаковый хэш-код");
    }
}
