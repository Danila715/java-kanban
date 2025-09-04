package main.java.main.manager;

import main.java.main.model.Task;

import java.util.*;

public class InMemoryHistoryManager implements HistoryManager {
    private Node<Task> head;
    private Node<Task> tail;
    private final Map<Integer, Node<Task>> nodeMap = new HashMap<>();

    static class Node<E> {
        E data;
        Node<E> next;
        Node<E> prev;

        Node(E data, Node<E> next, Node<E> prev) {
            this.data = data;
            this.next = next;
            this.prev = prev;
        }
    }

    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }
        int id = task.getId();
        if (nodeMap.containsKey(id)) {
            remove(id);
        }
        Node<Task> newNode = linkLast(task);
        nodeMap.put(id, newNode);
    }

    @Override
    public void remove(int id) {
        if (nodeMap.containsKey(id)) {
            removeNode(nodeMap.get(id));
            nodeMap.remove(id);
        }
    }

    @Override
    public List<Task> getHistory() {
        List<Task> list = new ArrayList<>();
        Node<Task> current = head;
        Set<Node<Task>> visited = new HashSet<>();
        while (current != null) {
            if (!visited.add(current)) {
                break;
            }
            list.add(current.data);
            current = current.next;
        }
        return list;
    }

    private Node<Task> linkLast(Task task) {
        Node<Task> newNode = new Node<>(task, null, tail);
        if (tail != null) {
            tail.next = newNode;
        }
        if (head == null) {
            head = newNode;
        }
        tail = newNode;
        return newNode;
    }

    private void removeNode(Node<Task> node) {
        if (node == null) {
            return;
        }
        // Обновляем указатели соседних узлов
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
            if (head != null) {
                head.prev = null; // Устанавливаем prev нового head в null
            }
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
        // Очищаем указатели удаляемого узла
        node.next = null;
        node.prev = null;
    }
}
