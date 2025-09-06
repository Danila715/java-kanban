package main.java.main.manager;

public class TaskOverlapException extends Exception {
    public TaskOverlapException(String message) {
        super(message);
    }

    public TaskOverlapException(String message, Throwable cause) {
        super(message, cause);
    }
}