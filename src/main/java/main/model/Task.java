package main.java.main.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class Task {

    private String title;
    private String description;
    private final int id;
    private TaskStatus status;
    private Duration duration; // Продолжительность задачи в минутах
    private LocalDateTime startTime; // Время начала выполнения

    //Основной конструктор
    public Task(String title, String description, int id, TaskStatus status) {
        this.title = title;
        this.description = description;
        this.id = id;
        this.status = status;
        this.duration = Duration.ZERO;
        this.startTime = null;
    }

    //Конструктор с временными параметрами
    public Task(String title, String description, int id, TaskStatus status, Duration duration, LocalDateTime startTime) {
        this.title = title;
        this.description = description;
        this.id = id;
        this.status = status;
        this.duration = duration != null ? duration : Duration.ZERO;
        this.startTime = startTime;
    }

    //Копирующий конструктор
    public Task(Task copy) {
        this.title = copy.title;
        this.description = copy.description;
        this.id = copy.id;
        this.status = copy.status;
        this.duration = copy.duration;
        this.startTime = copy.startTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration != null ? duration : Duration.ZERO;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    // Расчет времени завершения задачи
    public LocalDateTime getEndTime() {
        if (startTime == null || duration == null) {
            return null;
        }
        return startTime.plus(duration);
    }

    @Override
    public String toString() {
        return "Task{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", id=" + id +
                ", status=" + status +
                ", duration=" + duration.toMinutes() + " минут" +
                ", startTime=" + startTime +
                ", endTime=" + getEndTime() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}