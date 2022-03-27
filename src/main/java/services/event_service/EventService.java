package services.event_service;


import services.event_service.utils.EventListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventService {
    public static EventService INSTANCE;

    public static void initializeInstance(String... events) {
        if (INSTANCE != null)
            throw new RuntimeException("Instance has already been initialized");
        INSTANCE = new EventService(events);
    }

    public static EventService getInstance() {
        return INSTANCE;
    }

    private Map<String, List<EventListener>> listeners;

    private EventService(String... events) {
        listeners = new HashMap<>();
        for (String event : events) {
            listeners.put(event, new LinkedList<>());
        }
    }

    public void subscribe(String eventName, EventListener newSubscriber) {
        List<EventListener> subscribers = listeners.computeIfAbsent(eventName, k -> new LinkedList<>());
        //            throw new RuntimeException("No such event " + eventName);
        subscribers.add(newSubscriber);
    }

    public void unsubscribe(String eventName, EventListener newSubscriber) {
        List<EventListener> subscribers = listeners.get(eventName);
        if (subscribers != null)
            subscribers.remove(newSubscriber);
//            throw new RuntimeException("No such event " + eventName);
    }

    public void notify(String eventName, Object o) {
        List<EventListener> subscribers = listeners.computeIfAbsent(eventName, k -> new LinkedList<>());
//        if (subscribers == null)
//            throw new RuntimeException("No such event " + eventName);
        for (EventListener subscriber : subscribers) {
            subscriber.update(eventName, o);
        }
    }
}
