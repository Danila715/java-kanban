package main.manager;

import main.model.*;
import java.util.*;

public class InMemoryHistoryManager implements HistoryManager {
    private final List<Task> history = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 10;

    @Override
    public void add (Task task) {
        if (task != null) {
            history.add(task);
            if (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }
        }
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}
