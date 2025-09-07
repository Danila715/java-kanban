// В файл main/java/main/manager/NotFoundException.java (уже есть, но убедитесь, что он такой)
package main.java.main.manager;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}