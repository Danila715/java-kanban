package main.manager;

import main.model.*;
import java.util.ArrayList;

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
    ArrayList<SubTask> getSubTasksOfEpic(int epicId);
    void updateTask(Task updatedTask);
    void updateEpic(Epic updatedEpic);
    void updateSubTask(SubTask updatedSubTask);
    void deleteTaskById(int id);
    void deleteEpic(int id);
    void deleteSubTask(int id);
    ArrayList<Task> getAllTasks();
    ArrayList<Epic> getAllEpics();
    ArrayList<SubTask> getAllSubTasks();
    ArrayList<Task> getHistory();
}
