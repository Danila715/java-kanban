package main.manager;

import main.model.*;

import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final Map<Integer, Task> tasks = new HashMap<>();
    private int nextId = 1;
    private final HistoryManager historyManager;

    public InMemoryTaskManager() {
        this.historyManager = Managers.getDefaultHistory();
    }

    /*Создание задачи*/
    @Override
    public Task createTask(String title, String description, TaskStatus status) {
        int id = nextId++;
        Task task = new Task(title, description, id, status);
        tasks.put(id, task);
        return task;
    }

    /*Создание эпика*/
    @Override
    public void addEpic(String title, String description) {
        int id = getNextId();
        Epic epic = new Epic(title, description, id);
        epics.put(id, epic);
    }

    /*Добавление подзадачи*/
    @Override
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
    @Override
    public void deleteAllTasks() {
        tasks.clear();
    }

    /*Удаление всех эпиков*/
    @Override
    public void deleteAllEpics() {
        epics.clear();
        subTasks.clear();
    }

    /*Удаление всех подзадач*/
    @Override
    public void deleteAllSubTasks() {
        for (Epic epic : epics.values()) {
            epic.getSubTaskIds().clear();
        }
        subTasks.clear();
    }

    /*Получение задачи по id*/
    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    /*Получение эпика по id*/
    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    /*Получение подзадачи по id*/
    @Override
    public SubTask getSubTaskById(int id) {
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            historyManager.add(subTask);
        }
        return subTask;
    }


    /*Генерация id*/
    public int getNextId() {
        return nextId++;
    }

    /*Обновление nextId, чтобы избежать конфликтов*/
    public Task createTaskWithId(String title, String description, int id, TaskStatus status) {
        Task task = new Task(title, description, id, status);
        tasks.put(id, task);
        if (id >= nextId) {
            nextId = id + 1;
        }
        return task;
    }


    /*Получение списка всех подзадач эпика*/
    @Override
    public List<SubTask> getSubTasksOfEpic(int epicId) {
        List<SubTask> result = new ArrayList<>();
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

        List<Integer> subTaskIds = epic.getSubTaskIds();
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
    @Override
    public void updateTask(Task updatedTask) {
        int id = updatedTask.getId();
        if (tasks.containsKey(id)) {
            tasks.put(updatedTask.getId(), updatedTask);
        }
    }

    /*Обновление эпика*/
    @Override
    public void updateEpic (Epic epic) {
        if (epics.containsKey(epic.getId())) {
            Epic updatedEpic = epics.get(epic.getId());
            updatedEpic.setTitle(epic.getTitle());
            updatedEpic.setDescription(epic.getDescription());
            updateEpicStatus(updatedEpic);
        }
    }

    /*Обновление подзадачи*/
    @Override
    public void updateSubTask(SubTask subTask) {
        if (subTasks.containsKey(subTask.getId())) {
            subTasks.put(subTask.getId(), subTask);
            updateEpicStatus(epics.get(subTask.getEpicId()));
        }
    }

    /*Удаление задачи по id*/
    @Override
    public void deleteTaskById(int id) {
        tasks.remove(id);
    }

    /*Удаление эпика и всех его подзадач по id*/
    @Override
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
    @Override
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
    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /*Получение списка всех эпиков*/
    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    /*Получение списка всех подзадач*/
    @Override
    public List<SubTask> getAllSubTasks() {
        return new ArrayList<>(subTasks.values());
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }
}
