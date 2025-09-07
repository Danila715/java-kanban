package main.java.main.manager;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}