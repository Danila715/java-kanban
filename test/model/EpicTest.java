package model;

import main.java.main.model.Epic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class EpicTest {
    @Test
    void epicsWithSameIdAreEqual() {
        Epic epic1 = new Epic("Эпик 1", "Описание 1", 1);
        Epic epic2 = new Epic("Эпик 2", "Описание 2", 1);
        assertEquals(epic1, epic2, "Эпики с одинаковым ID должны быть равны");
        assertNotSame(epic1, epic2, "Эпики не должны быть одним и тем же объектом");
        assertEquals(epic1.hashCode(), epic2.hashCode(), "Эпики с одинаковым ID должны иметь одинаковый хэш-код");
    }
}