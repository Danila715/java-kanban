package main.manager;

import main.model.*;
import java.util.*;

public interface HistoryManager {
    void add(Task task);
    void remove(int id);
    List<Task> getHistory();
}