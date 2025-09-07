package main.java.main.manager;

import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

public class InMemoryTaskManager implements TaskManager {
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, SubTask> subTasks = new HashMap<>();
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected int nextId = 1;
    private final HistoryManager historyManager;
    // Структура для хранения задач, отсортированных по времени
    protected final Set<Task> prioritizedTasks = new TreeSet<>((task1, task2) -> {
        if (task1.getStartTime() == null && task2.getStartTime() == null) {
            return Integer.compare(task1.getId(), task2.getId());
        }
        if (task1.getStartTime() == null) return 1;
        if (task2.getStartTime() == null) return -1;
        int timeComparison = task1.getStartTime().compareTo(task2.getStartTime());
        if (timeComparison != 0) return timeComparison;
        return Integer.compare(task1.getId(), task2.getId());
    });

    public InMemoryTaskManager() {
        this.historyManager = Managers.getDefaultHistory();
    }

    private void validateTaskOverlap(Task task) throws TaskOverlapException {
        if (task.getStartTime() != null && hasOverlapWithExistingTasks(task)) {
            throw new TaskOverlapException("Задача пересекается по времени с существующими задачами");
        }
    }

    /*Создание задачи*/
    @Override
    public Task createTask(String title, String description, TaskStatus status) throws TaskOverlapException {
        return createTask(title, description, status, Duration.ZERO, null);
    }

    @Override
    public Task createTask(String title, String description, TaskStatus status, Duration duration, LocalDateTime startTime) throws TaskOverlapException {
        int id = nextId++;
        Task task = new Task(title, description, id, status, duration, startTime);
        validateTaskOverlap(task);
        tasks.put(id, new Task(task));
        if (startTime != null) {
            prioritizedTasks.add(new Task(task));
        }
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
    public void addSubTask(String title, String description, int epicId, TaskStatus status) throws TaskOverlapException {
        addSubTask(title, description, epicId, status, Duration.ZERO, null);
    }

    @Override
    public void addSubTask(String title, String description, int epicId, TaskStatus status, Duration duration, LocalDateTime startTime) throws TaskOverlapException {
        Epic epic = epics.get(epicId);
        if (epic != null) {
            int id = getNextId();
            if (id == epicId) {
                return;
            }
            SubTask subTask = new SubTask(title, description, id, status, epicId, duration, startTime);
            validateTaskOverlap(subTask);
            subTasks.put(id, new SubTask(subTask));
            epic.addSubTaskId(id);
            updateEpicStatus(epic);
            updateEpicFields(epic);
            if (startTime != null) {
                prioritizedTasks.add(new SubTask(subTask));
            }
        }
    }

    /*Удаление всех задач*/
    @Override
    public void deleteAllTasks() {
        for (Task task : tasks.values()) {
            historyManager.remove(task.getId());
            prioritizedTasks.remove(task);
        }
        tasks.clear();
    }

    /*Удаление всех эпиков*/
    @Override
    public void deleteAllEpics() {
        for (Epic epic : epics.values()) {
            historyManager.remove(epic.getId());
        }
        epics.clear();
        for (SubTask subTask : subTasks.values()) {
            historyManager.remove(subTask.getId());
            prioritizedTasks.remove(subTask);
        }
        subTasks.clear();
    }

    /*Удаление всех подзадач*/
    @Override
    public void deleteAllSubTasks() {
        for (Epic epic : epics.values()) {
            epic.getSubTaskIds().clear();
            updateEpicStatus(epic);
            updateEpicFields(epic);
        }
        for (SubTask subTask : subTasks.values()) {
            historyManager.remove(subTask.getId());
            prioritizedTasks.remove(subTask);
        }
        subTasks.clear();
    }

    /*Получение задачи по id*/
    @Override
    public Task getTaskById(int id) throws NotFoundException {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(new Task(task));
            return new Task(task);
        }
        throw new NotFoundException("Задача с ID " + id + " не найдена");
    }

    /*Получение эпика по id*/
    @Override
    public Epic getEpicById(int id) throws NotFoundException {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
            return epic;
        }
        throw new NotFoundException("Эпик с ID " + id + " не найден");
    }

    /*Получение подзадачи по id*/
    @Override
    public SubTask getSubTaskById(int id) throws NotFoundException {
        SubTask subTask = subTasks.get(id);
        if (subTask != null) {
            historyManager.add(new SubTask(subTask));
            return new SubTask(subTask);
        }
        throw new NotFoundException("Подзадача с ID " + id + " не найдена");
    }

    /*Генерация id*/
    public int getNextId() {
        return nextId++;
    }

    /*Обновление nextId, чтобы избежать конфликтов*/
    // Note: This method seems unused or potentially problematic in its original form.
    // It's kept as is based on the provided file.
    public Task createTaskWithId(String title, String description, int id, TaskStatus status) throws TaskOverlapException {
        return createTaskWithId(title, description, id, status, Duration.ZERO, null);
    }

    public Task createTaskWithId(String title, String description, int id, TaskStatus status, Duration duration, LocalDateTime startTime) throws TaskOverlapException {
        Task task = new Task(title, description, id, status, duration, startTime);
        validateTaskOverlap(task);
        tasks.put(id, new Task(task));
        if (id >= nextId) {
            nextId = id + 1;
        }
        if (startTime != null) {
            prioritizedTasks.add(new Task(task));
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
    void updateEpicStatus(Epic epic) {
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

    /*Обновление полей эпика на основе подзадач*/
    private void updateEpicFields(Epic epic) {
        if (epic == null) {
            return;
        }
        List<SubTask> epicSubTasks = getSubTasks(epic.getId());
        epic.calculateEpicFields(epicSubTasks);
    }

    /*Обновление задачи*/
    @Override
    public void updateTask(Task updatedTask) throws TaskOverlapException, NotFoundException {
        if (updatedTask == null) {
            throw new NotFoundException("Переданная задача равна null");
        }
        if (!tasks.containsKey(updatedTask.getId())) {
            throw new NotFoundException("Задача с ID " + updatedTask.getId() + " не найдена для обновления");
        }

        Task oldTask = tasks.get(updatedTask.getId());
        if (updatedTask.getStartTime() != null &&
                !Objects.equals(oldTask.getStartTime(), updatedTask.getStartTime()) &&
                hasOverlapWithExistingTasks(updatedTask)) {
            throw new TaskOverlapException("Обновленная задача пересекается по времени с существующими задачами");
        }
        prioritizedTasks.remove(oldTask);
        tasks.put(updatedTask.getId(), new Task(updatedTask));
        if (updatedTask.getStartTime() != null) {
            prioritizedTasks.add(new Task(updatedTask));
        }
    }

    /*Обновление эпика*/
    @Override
    public void updateEpic(Epic epic) throws NotFoundException {
        if (epic == null) {
            throw new NotFoundException("Переданный эпик равен null");
        }
        if (!epics.containsKey(epic.getId())) {
            throw new NotFoundException("Эпик с ID " + epic.getId() + " не найден для обновления");
        }

        Epic updatedEpic = epics.get(epic.getId());
        updatedEpic.setTitle(epic.getTitle());
        updatedEpic.setDescription(epic.getDescription());
        updateEpicStatus(updatedEpic);
        updateEpicFields(updatedEpic);
    }

    /*Обновление подзадачи*/
    @Override
    public void updateSubTask(SubTask subTask) throws TaskOverlapException, NotFoundException {
        if (subTask == null) {
            throw new NotFoundException("Переданная подзадача равна null");
        }
        if (!subTasks.containsKey(subTask.getId())) {
            throw new NotFoundException("Подзадача с ID " + subTask.getId() + " не найдена для обновления");
        }
        if (subTask.getId() == subTask.getEpicId()) {
            // Consider throwing a specific exception or handling this case differently
            return;
        }

        SubTask oldSubTask = subTasks.get(subTask.getId());
        if (subTask.getStartTime() != null &&
                !Objects.equals(oldSubTask.getStartTime(), subTask.getStartTime()) &&
                hasOverlapWithExistingTasks(subTask)) {
            throw new TaskOverlapException("Обновленная подзадача пересекается по времени с существующими задачами");
        }
        prioritizedTasks.remove(oldSubTask);
        subTasks.put(subTask.getId(), new SubTask(subTask));
        Epic epic = epics.get(subTask.getEpicId());
        if (epic != null) { // Defensive check, though epic should exist if subtask was valid
            updateEpicStatus(epic);
            updateEpicFields(epic);
        }
        if (subTask.getStartTime() != null) {
            prioritizedTasks.add(new SubTask(subTask));
        }
    }

    /*Удаление задачи по id*/
    @Override
    public void deleteTaskById(int id) throws NotFoundException {
        Task task = tasks.remove(id);
        if (task != null) {
            prioritizedTasks.remove(task);
            historyManager.remove(id);
        } else {
            throw new NotFoundException("Задача с ID " + id + " не найдена для удаления");
        }
    }

    /*Удаление эпика и всех его подзадач по id*/
    @Override
    public void deleteEpic(int id) throws NotFoundException {
        if (epics.containsKey(id)) {
            Epic epic = epics.get(id);
            for (Integer subTaskId : epic.getSubTaskIds()) {
                SubTask subTask = subTasks.remove(subTaskId);
                if (subTask != null) {
                    prioritizedTasks.remove(subTask);
                }
                historyManager.remove(subTaskId);
            }
            epics.remove(id);
            historyManager.remove(id);
        } else {
            throw new NotFoundException("Эпик с ID " + id + " не найден для удаления");
        }
    }

    /*Удаление подзадачи по id*/
    @Override
    public void deleteSubTask(int id) throws NotFoundException {
        SubTask subTask = subTasks.remove(id);
        if (subTask != null) {
            prioritizedTasks.remove(subTask);
            Epic epic = epics.get(subTask.getEpicId());
            if (epic != null) {
                epic.removeSubTaskId(id);
                updateEpicStatus(epic);
                updateEpicFields(epic);
            }
            historyManager.remove(id);
        } else {
            throw new NotFoundException("Подзадача с ID " + id + " не найдена для удаления");
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

    /*Получение задач в порядке приоритета (по времени начала)*/
    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    /*Проверка пересечения двух задач по времени*/
    @Override
    public boolean checkTaskOverlap(Task task1, Task task2) {
        if (task1 == null || task2 == null ||
                task1.getStartTime() == null || task2.getStartTime() == null ||
                task1.getEndTime() == null || task2.getEndTime() == null) {
            return false;
        }
        // Два отрезка НЕ пересекаются, если конец первого <= начала второго или конец второго <= начала первого
        return !(task1.getEndTime().isBefore(task2.getStartTime()) || task1.getEndTime().equals(task2.getStartTime()) ||
                task2.getEndTime().isBefore(task1.getStartTime()) || task2.getEndTime().equals(task1.getStartTime()));
    }

    /*Проверка пересечения задачи с любой другой в менеджере*/
    @Override
    public boolean hasOverlapWithExistingTasks(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getEndTime() == null) {
            return false;
        }
        return Stream.concat(
                        Stream.concat(
                                tasks.values().stream().filter(task -> task.getId() != newTask.getId()),
                                subTasks.values().stream().filter(task -> task.getId() != newTask.getId())
                        ),
                        Stream.empty() // This part seems redundant from the original, kept as is.
                )
                .anyMatch(existingTask -> checkTaskOverlap(newTask, existingTask));
    }
}