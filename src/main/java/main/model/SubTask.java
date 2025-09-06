package main.java.main.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class SubTask extends Task {

    private int epicId;

    //Основной конструктор
    public SubTask(String title, String description, int id, TaskStatus status, int epicId) {
        super(title, description, id, status);
        this.epicId = epicId;
    }

    //Конструктор с временными параметрами
    public SubTask(String title, String description, int id, TaskStatus status, int epicId,
                   Duration duration, LocalDateTime startTime) {
        super(title, description, id, status, duration, startTime);
        this.epicId = epicId;
    }

    //Копирующий конструктор
    public SubTask(SubTask copy) {
        super(copy);
        this.epicId = copy.epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }

    @Override
    public String toString() {
        return "SubTask{" +
                "title='" + getTitle() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", id=" + getId() +
                ", status=" + getStatus() +
                ", epicId=" + epicId +
                ", duration=" + getDuration().toMinutes() + " минут" +
                ", startTime=" + getStartTime() +
                ", endTime=" + getEndTime() +
                '}';
    }
}