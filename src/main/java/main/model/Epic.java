package main.java.main.model;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Integer> subTaskIds;

    public Epic(String title, String description, int id) {
        super(title, description, id, TaskStatus.NEW);
        this.subTaskIds = new ArrayList<>();
    }

    public List<Integer> getSubTaskIds() {
        return subTaskIds;
    }

    public void addSubTaskId(int subTaskId) {
        subTaskIds.add(subTaskId);
    }

    public void removeSubTaskId(int subTaskId) {
        subTaskIds.remove((Integer) subTaskId);
    }

    public void clearSubTasks() {
        subTaskIds.clear();
    }

    public List<SubTask> getSubTasks(int epicId) {
        return new ArrayList<>();
    }
}
