package main.java.main.manager;

import java.io.File;
import java.io.IOException;

public class Managers {
    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }

    public static TaskManager getDefaultFileBacked() {
        try {
            File file = File.createTempFile("tasks", ".csv");
            return FileBackedTaskManager.loadFromFile(file);
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при создании временного файла для FileBackedTaskManager", e);
        }

    }
}
