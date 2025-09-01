package main.java.main.manager;

import main.java.main.model.Task;

import java.util.*;

public interface HistoryManager {
    void add(Task task);

    void remove(int id);

    List<Task> getHistory();
}