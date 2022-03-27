package services.event_service.utils;

public interface EventListener extends java.util.EventListener {
    void update(String eventName, Object o);
}
