package main.java.main.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Integer> subTaskIds;
    private LocalDateTime endTime; // Расчетное время завершения

    public Epic(String title, String description, int id) {
        super(title, description, id, TaskStatus.NEW);
        this.subTaskIds = new ArrayList<>();
        this.endTime = null;
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

    // Расчет параметров эпика на основе его подзадач
    public void calculateEpicFields(List<SubTask> subTasks) {
        if (subTasks.isEmpty()) {
            setDuration(Duration.ZERO);
            setStartTime(null);
            this.endTime = null;
            return;
        }

        // Рассчитываем общую продолжительность
        Duration totalDuration = Duration.ZERO;
        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;

        for (SubTask subTask : subTasks) {
            // Суммируем продолжительности
            if (subTask.getDuration() != null) {
                totalDuration = totalDuration.plus(subTask.getDuration());
            }

            // Ищем самое раннее время начала
            if (subTask.getStartTime() != null) {
                if (earliestStart == null || subTask.getStartTime().isBefore(earliestStart)) {
                    earliestStart = subTask.getStartTime();
                }

                // Ищем самое позднее время завершения
                LocalDateTime subTaskEnd = subTask.getEndTime();
                if (subTaskEnd != null) {
                    if (latestEnd == null || subTaskEnd.isAfter(latestEnd)) {
                        latestEnd = subTaskEnd;
                    }
                }
            }
        }

        setDuration(totalDuration);
        setStartTime(earliestStart);
        this.endTime = latestEnd;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "Epic{" +
                "title='" + getTitle() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", id=" + getId() +
                ", status=" + getStatus() +
                ", subTaskIds=" + subTaskIds +
                ", duration=" + getDuration().toMinutes() + " минут" +
                ", startTime=" + getStartTime() +
                ", endTime=" + getEndTime() +
                '}';
    }
}