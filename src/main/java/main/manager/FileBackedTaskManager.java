package main.java.main.manager;

import main.java.main.model.Epic;
import main.java.main.model.SubTask;
import main.java.main.model.Task;
import main.java.main.model.TaskStatus;

import java.io.*;

//Менеджер задач с сохранением в файл
public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    private FileBackedTaskManager(File file) {
        super();
        this.file = file;
    }

    //Сохранение состояния в CSV файл
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            writer.write("id,type,name,status,description,epic");
            writer.newLine();

            for (Task task : tasks.values()) {
                writer.write(toCsv(task));
                writer.newLine();
            }

            for (Epic epic : epics.values()) {
                writer.write(toCsv(epic));
                writer.newLine();
            }

            for (SubTask subTask : subTasks.values()) {
                writer.write(toCsv(subTask));
                writer.newLine();
            }

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении в файл: " + file.getPath(), e);
        }
    }

    //Преобразование задачи в CSV строку
    private String toCsv(Task task) {
        String type = task instanceof SubTask ? "SUBTASK" : task instanceof Epic ? "EPIC" : "TASK";
        String epicId = task instanceof SubTask ? String.valueOf(((SubTask) task).getEpicId()) : "";
        return String.format("%d,%s,%s,%s,%s,%s",
                task.getId(), type, task.getTitle(), task.getStatus(), task.getDescription(), epicId);
    }

    //Загрузка менеджера из файла
    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(",");
                    int id = Integer.parseInt(parts[0]);
                    String type = parts[1];
                    String title = parts[2];
                    TaskStatus status = TaskStatus.valueOf(parts[3]);
                    String description = parts[4];
                    int epicId = parts.length > 5 && !parts[5].isEmpty() ? Integer.parseInt(parts[5]) : 0;

                    switch (type) {
                        case "TASK":
                            manager.tasks.put(id, new Task(title, description, id, status));
                            if (id >= manager.nextId) {
                                manager.nextId = id + 1;
                            }
                            break;
                        case "EPIC":
                            manager.epics.put(id, new Epic(title, description, id));
                            if (id >= manager.nextId) {
                                manager.nextId = id + 1;
                            }
                            break;
                        case "SUBTASK":
                            manager.subTasks.put(id, new SubTask(title, description, id, status, epicId));
                            Epic epic = manager.epics.get(epicId);
                            if (epic != null) {
                                epic.addSubTaskId(id);
                                manager.updateEpicStatus(epic);
                            }
                            if (id >= manager.nextId) {
                                manager.nextId = id + 1;
                            }
                            break;
                    }
                }
            }

        } catch (IOException e) {
            throw new ManagerLoadException("Ошибка при загрузке файла: " + file.getPath(), e);
        }
        return manager;
    }

    @Override
    public Task createTask(String title, String description, TaskStatus status) {
        Task task = super.createTask(title, description, status);
        save();
        return task;
    }

    @Override
    public void addEpic(String title, String description) {
        super.addEpic(title, description);
        save();
    }

    @Override
    public void addSubTask(String title, String description, int epicId, TaskStatus status) {
        super.addSubTask(title, description, epicId, status);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public void deleteAllSubTasks() {
        super.deleteAllSubTasks();
        save();
    }

    @Override
    public Task getTaskById(int id) {
        Task task = super.getTaskById(id);
        save();
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = super.getEpicById(id);
        save();
        return epic;
    }

    @Override
    public SubTask getSubTaskById(int id) {
        SubTask subTask = super.getSubTaskById(id);
        save();
        return subTask;
    }

    @Override
    public void updateTask(Task updatedTask) {
        super.updateTask(updatedTask);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubTask(SubTask subTask) {
        super.updateSubTask(subTask);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save();
    }

    @Override
    public void deleteSubTask(int id) {
        super.deleteSubTask(id);
        save();
    }


}
