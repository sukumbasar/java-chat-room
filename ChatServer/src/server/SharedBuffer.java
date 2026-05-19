package server;

import java.util.LinkedList;
import java.util.Queue;

public class SharedBuffer {

    private final Queue<String> buffer = new LinkedList<>();
    private final int maxSize;

    public SharedBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized void produce(String request) throws InterruptedException {
        while (buffer.size() >= maxSize) {
            wait();
        }
        buffer.add(request);
        notifyAll();
    }

    public synchronized String consume() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait();
        }
        String request = buffer.poll();
        notifyAll();
        return request;
    }
}