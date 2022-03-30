package bot.trip_update_handler;

import models.dao.SendTripDao;

import java.util.LinkedList;
import java.util.Queue;

public class ConcurrentTripQueue {
    private final Queue<SendTripDao> tripQueue = new LinkedList<>();
    private final int maxSize;
    private final Object FULL_QUEUE = new Object();
    private final Object EMPTY_QUEUE = new Object();

    public ConcurrentTripQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public void waitOnFull() throws InterruptedException {
        synchronized (FULL_QUEUE) {
            FULL_QUEUE.wait();
        }
    }

    public void notifyAllForFull() {
        synchronized (FULL_QUEUE) {
            FULL_QUEUE.notifyAll();
        }
    }

    public void waitOnEmpty() throws InterruptedException {
        synchronized (EMPTY_QUEUE) {
            EMPTY_QUEUE.wait();
        }
    }

    public void notifyAllForEmpty() {
        synchronized (EMPTY_QUEUE) {
            EMPTY_QUEUE.notify();
        }
    }

    public void add(SendTripDao trip) {
        tripQueue.add(trip);
    }

    public SendTripDao remove() {
        return tripQueue.poll();
    }

    public boolean isFull() {
        return tripQueue.size() == maxSize;
    }

    public boolean isEmpty() {
        return tripQueue.isEmpty();
    }
}
