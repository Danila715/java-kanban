package main.java.main.manager;

import main.java.main.model.*;

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
        tasks.put(id, new Task(task));
        return new Task(task);
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
            if (id == epicId) {
                return;
            }
            SubTask subTask = new SubTask(title, description, id, status, epicId);
            subTasks.put(id, new SubTask(subTask));
            epic.addSubTaskId(id);
            updateEpicStatus(epic);
        }
    }

    /*Удаление всех задач*/
    @Override
    public void deleteAllTasks() {
        for (Integer id : new ArrayList<>(tasks.keySet())) {
            historyManager.remove(id);
        }
        tasks.clear();
    }

    /*Удаление всех эпиков*/
    @Override
    public void deleteAllEpics() {
        for (Integer id : new ArrayList<>(epics.keySet())) {
            historyManager.remove(id);
        }
        epics.clear();
        for (Integer id : new ArrayList<>(subTasks.keySet())) {
            historyManager.remove(id);
        }
        subTasks.clear();
    }

    /*Удаление всех подзадач*/
    @Override
    public void deleteAllSubTasks() {
        for (Epic epic : epics.values()) {
            epic.getSubTaskIds().clear();
            updateEpicStatus(epic);
        }
        for (Integer id : new ArrayList<>(subTasks.keySet())) {
            historyManager.remove(id);
        }
        subTasks.clear();
    }

    /*Получение задачи по id*/
    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(new Task(task));
            return new Task(task);
        }
        return null;
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
            historyManager.add(new SubTask(subTask));
            return new SubTask(subTask);
        }
        return null;
    }


    /*Генерация id*/
    public int getNextId() {
        return nextId++;
    }

    /*Обновление nextId, чтобы избежать конфликтов*/
    public Task createTaskWithId(String title, String description, int id, TaskStatus status) {
        Task task = new Task(title, description, id, status);
        tasks.put(id, new Task(task));
        if (id >= nextId) {
            nextId = id + 1;
        }
        return new Task(task);
    }


    /*Получение списка всех подзадач эпика*/
    @Override
    public List<SubTask> getSubTasks(int epicId) {
        List<SubTask> result = new ArrayList<>();
        Epic epic = epics.get(epicId);
        if (epic != null) {
            for (int subTaskId : epic.getSubTaskIds()) {
                SubTask subTask = subTasks.get(subTaskId);
                if (subTask != null) {
                    result.add(new SubTask(subTask));
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

        Collection<TaskStatus> uniqueStatuses = new HashSet<>();
        for (int subTaskId : subTaskIds) {
            SubTask subTask = subTasks.get(subTaskId);
            if (subTask != null) {
                uniqueStatuses.add(subTask.getStatus());
            }
        }

        if (uniqueStatuses.size() == 1) {
            epic.setStatus(uniqueStatuses.iterator().next());
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    /*Обновление задачи*/
    @Override
    public void updateTask(Task updatedTask) {
        if (updatedTask != null) {
            tasks.put(updatedTask.getId(), new Task(updatedTask));
        }
    }

    /*Обновление эпика*/
    @Override
    public void updateEpic(Epic epic) {
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
            if (subTask.getId() == subTask.getEpicId()) {
                return;
            }
            subTasks.put(subTask.getId(), new SubTask(subTask));
            updateEpicStatus(epics.get(subTask.getEpicId()));
        }
    }

    /*Удаление задачи по id*/
    @Override
    public void deleteTaskById(int id) {
        tasks.remove(id);
        historyManager.remove(id);
    }

    /*Удаление эпика и всех его подзадач по id*/
    @Override
    public void deleteEpic(int id) {
        if (epics.containsKey(id)) {
            Epic epic = epics.get(id);
            for (Integer subTaskId : epic.getSubTaskIds()) {
                subTasks.remove(subTaskId);
                historyManager.remove(subTaskId);
            }
            epics.remove(id);
            historyManager.remove(id);
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
            historyManager.remove(id);
        }
    }

    /*Получение списка всех задач*/
    @Override
    public List<Task> getAllTasks() {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks.values()) {
            result.add(new Task(task));
        }
        return result;
    }

    /*Получение списка всех эпиков*/
    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    /*Получение списка всех подзадач*/
    @Override
    public List<SubTask> getAllSubTasks() {
        List<SubTask> result = new ArrayList<>();
        for (SubTask subTask : subTasks.values()) {
            result.add(new SubTask(subTask));
        }
        return result;
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }
}
