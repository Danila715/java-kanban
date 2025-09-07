package main.java.main.manager;

import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskManager {
    Task createTask(String title, String description, TaskStatus status) throws TaskOverlapException;

    Task createTask(String title, String description, TaskStatus status, Duration duration, LocalDateTime startTime) throws TaskOverlapException;

    void deleteAllTasks();

    void deleteAllEpics();

    void deleteAllSubTasks();

    Task getTaskById(int id) throws NotFoundException;
    Epic getEpicById(int id) throws NotFoundException;
    SubTask getSubTaskById(int id) throws NotFoundException;

    void addEpic(String title, String description);

    void addSubTask(String title, String description, int epicId, TaskStatus status) throws TaskOverlapException;

    void addSubTask(String title, String description, int epicId, TaskStatus status, Duration duration, LocalDateTime startTime) throws TaskOverlapException;

    List<SubTask> getSubTasks(int epicId);

    void updateTask(Task updatedTask) throws TaskOverlapException;

    void updateEpic(Epic updatedEpic);

    void updateSubTask(SubTask updatedSubTask) throws TaskOverlapException;

    void deleteTaskById(int id);

    void deleteEpic(int id);

    void deleteSubTask(int id);

    List<Task> getAllTasks();

    List<Epic> getAllEpics();

    List<SubTask> getAllSubTasks();

    List<Task> getHistory();

    List<Task> getPrioritizedTasks();

    boolean checkTaskOverlap(Task task1, Task task2);

    boolean hasOverlapWithExistingTasks(Task task);
}