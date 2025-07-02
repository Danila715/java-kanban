package model;

import java.util.ArrayList;

public class Epic extends Task {
    private final ArrayList<Integer> subTaskIds;

    public Epic(String title, String description, int id) {
        super(title, description, id, TaskStatus.NEW);
        this.subTaskIds = new ArrayList<>();
    }

    public ArrayList<Integer> getSubTaskIds() {
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

    public ArrayList<SubTask> getSubTasks(int epicId) {
        return new ArrayList<>();
    }
}
