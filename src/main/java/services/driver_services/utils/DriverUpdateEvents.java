package services.driver_services.utils;

public interface DriverUpdateEvents {
    void onDriverQueueEmptyEvent();
    void onDriverQueueNotEmptyEvent();
}
