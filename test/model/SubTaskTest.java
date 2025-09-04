package model;

import main.java.main.model.SubTask;
import main.java.main.model.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class SubTaskTest {
    @Test
    void subTasksWithSameIdAreEqual() {
        SubTask subTask1 = new SubTask("Подзадача 1", "Описание 1", 1, TaskStatus.NEW, 1);
        SubTask subTask2 = new SubTask("Подзадача 2", "Описание 2", 1, TaskStatus.IN_PROGRESS, 2);
        assertEquals(subTask1, subTask2, "Подзадачи с одинаковым ID должны быть равны");
        assertNotSame(subTask1, subTask2, "Подзадачи не должны быть одним и тем же объектом");
        assertEquals(subTask1.hashCode(), subTask2.hashCode(), "Подзадачи с одинаковым ID должны иметь одинаковый хэш-код");
    }
}
