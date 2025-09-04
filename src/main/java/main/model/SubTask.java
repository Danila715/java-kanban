package main.java.main.model;

public class SubTask extends Task {

    private int epicId;

    //Основной конструктор
    public SubTask(String title, String description, int id, TaskStatus status, int epicId) {
        super(title, description, id, status);
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

}
