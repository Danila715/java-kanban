package main.java.main.manager;

import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;

import java.util.*;

public interface TaskManager {
    Task createTask(String title, String description, TaskStatus status);

    void deleteAllTasks();

    void deleteAllEpics();

    void deleteAllSubTasks();

    Task getTaskById(int id);

    Epic getEpicById(int id);

    SubTask getSubTaskById(int id);

    void addEpic(String title, String description);

    void addSubTask(String title, String description, int epicId, TaskStatus status);

    List<SubTask> getSubTasks(int epicId);

    void updateTask(Task updatedTask);

    void updateEpic(Epic updatedEpic);

    void updateSubTask(SubTask updatedSubTask);

    void deleteTaskById(int id);

    void deleteEpic(int id);

    void deleteSubTask(int id);

    List<Task> getAllTasks();

    List<Epic> getAllEpics();

    List<SubTask> getAllSubTasks();

    List<Task> getHistory();
}
