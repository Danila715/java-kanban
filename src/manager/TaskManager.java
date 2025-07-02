package manager;

import model.*;

import java.util.ArrayList;
import java.util.HashMap;

public class TaskManager {
    private final HashMap<Integer, Epic> epics = new HashMap<>();
    private final HashMap<Integer, SubTask> subTasks = new HashMap<>();
    private final HashMap<Integer, Task> tasks = new HashMap<>();
    private int nextId = 1;

    /*Создание задачи*/
    public Task createTask(String title, String description, TaskStatus status) {
        int id = nextId++;
        Task task = new Task(title, description, id, status);
        tasks.put(id, task);
        return task;
    }

    /*Создание эпика*/
    public void addEpic(String title, String description) {
        int id = getNextId();
        Epic epic = new Epic(title, description, id);
        epics.put(id, epic);
    }

    /*Добавление подзадачи*/
    public void addSubTask(String title, String description, int epicId, TaskStatus status) {
        Epic epic = epics.get(epicId);
        if (epic != null) {
            int id = getNextId();
            SubTask subTask = new SubTask(title, description, id, status, epicId);
            subTasks.put(id, subTask);
            epic.addSubTaskId(id);
            updateEpicStatus(epic);
        }
    }

    /*Удаление всех задач*/
    public void deleteAllTasks() {
        tasks.clear();
    }

    /*Удаление всех эпиков*/
    public void deleteAllEpics() {
        epics.clear();
        subTasks.clear();
    }

    /*Удаление всех подзадач*/
    public void deleteAllSubTasks() {
        for (Epic epic : epics.values()) {
            epic.getSubTaskIds().clear();
        }
        subTasks.clear();
    }

    /*Получение задачи по id*/
    public Task getTaskById(int id) {
        return tasks.get(id);
    }

    /*Получение эпика по id*/
    public Epic getEpicById(int id) {
        return epics.get(id);
    }

    /*Получение подзадачи по id*/
    public SubTask getSubTaskById(int id) {
        return subTasks.get(id);
    }


    /*Генерация id*/
    private int getNextId() {
        return nextId++;
    }

    /*Получение списка всех подзадач эпика*/
    public ArrayList<SubTask> getSubTasks(int epicId) {
        ArrayList<SubTask> result = new ArrayList<>();
        Epic epic = epics.get(epicId);
        if (epic != null) {
            for (int subTaskId : epic.getSubTaskIds()) {
                SubTask subTask = subTasks.get(subTaskId);
                if (subTask != null) {
                    result.add(subTask);
                }
            }
        }
        return result;
    }

    /*Обновление статуса эпика*/
    private void updateEpicStatus(Epic epic) {
        if (epic == null) {
            return;
        }

        ArrayList<Integer> subTaskIds = epic.getSubTaskIds();
        if (subTaskIds.isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (int subTaskId : subTaskIds) {
            SubTask subTask = subTasks.get(subTaskId);
            if (subTask == null) {
                continue;
            }
            TaskStatus status = subTask.getStatus();
            if (status != TaskStatus.NEW) {
                allNew = false;
            }
            if (status != TaskStatus.DONE) {
                allDone = false;
            }
        }
        if (allDone) {
            epic.setStatus(TaskStatus.DONE);
        } else if (allNew) {
            epic.setStatus(TaskStatus.NEW);
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    /*Обновление задачи*/
    public void updateTask(Task updatedTask) {
        int id = updatedTask.getId();
        if (tasks.containsKey(id)) {
            tasks.put(id, updatedTask);
        }
    }

    /*Обновление эпика*/
    public void updateEpic (Epic epic) {
        if (epics.containsKey(epic.getId())) {
            Epic updatedEpic = epics.get(epic.getId());
            updatedEpic.setTitle(epic.getTitle());
            updatedEpic.setDescription(epic.getDescription());
            updateEpicStatus(updatedEpic);
        }
    }

    /*Обновление подзадачи*/
    public void updateSubTask(SubTask subTask) {
        if (subTasks.containsKey(subTask.getId())) {
            subTasks.put(subTask.getId(), subTask);
            updateEpicStatus(epics.get(subTask.getEpicId()));
        }
    }

    /*Удаление задачи по id*/
    public void deleteTaskById(int id) {
        tasks.remove(id);
    }

    /*Удаление эпика и всех его подзадач по id*/
    public void deleteEpic(int id) {
        if (epics.containsKey(id)) {
            Epic epic = epics.get(id);
            for (Integer subTaskId : epic.getSubTaskIds()) {
                subTasks.remove(subTaskId);
            }
            epics.remove(id);
        }
    }

    /*Удаление подзадачи по id*/
    public void deleteSubTask(int id) {
        SubTask subTask = subTasks.remove(id);
        if (subTask != null) {
            Epic epic = epics.get(subTask.getEpicId());
            if (epic != null) {
                epic.removeSubTaskId(id);
                updateEpicStatus(epic);
            }
        }
    }

    /*Получение списка всех задач*/
    public ArrayList<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /*Получение списка всех эпиков*/
    public ArrayList<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    /*Получение списка всех подзадач*/
    public ArrayList<SubTask> getAllSubTasks() {
        return new ArrayList<>(subTasks.values());
    }
}
