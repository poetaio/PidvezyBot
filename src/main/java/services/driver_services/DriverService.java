package services.driver_services;

import models.dao.DriverUpdateDao;
import services.driver_services.utils.DriverUpdateEvents;
import services.trip_services.TripQueueService;

import java.util.*;

/**
 * Service to manage all users who have hit "Я волонтер"
 */
public class DriverService {
    private final List<Long> driverList;

    private final DriverViewUpdateService driverViewUpdateService;

    public DriverService(List<Long> driverList, List<DriverUpdateDao> driverToUpdateList,
                         DriverUpdateEvents driverUpdateEvents) {
        this.driverList = driverList;
        driverViewUpdateService = new DriverViewUpdateService(driverToUpdateList){
            @Override
            protected void onDriverQueueEmptyEvent() {
                driverUpdateEvents.onDriverQueueEmptyEvent();
            }

            @Override
            protected void onDriverQueueNotEmptyEvent() {
                driverUpdateEvents.onDriverQueueNotEmptyEvent();;
            }
        };
    }

    /**
     * Removes driver from list and unsubscribes him from updates
     * @param driverChatId driver chat id
     */
    public void removeDriver(long driverChatId) {
        driverList.remove(driverChatId);
        driverViewUpdateService.removeDriver(driverChatId);
        TripQueueService.getInstance().unsetView(driverChatId);
    }

    /**
     * Adds drivers to list, and subscribe him for view update
     * @param driverChatId driver's chat id with bot
     */
    public void addDriver(long driverChatId) {
        if (driverList.contains(driverChatId))
            return;

        driverList.add(driverChatId);
        driverViewUpdateService.addDriver(driverChatId);
    }

    // driverUpdateService methods

    /**
     * Resets time of the next view update when driver his "Наступний запит"
     * @param driverChatId driver chat id
     */
    public void resetDriverTime(long driverChatId) {
        driverViewUpdateService.resetDriverTime(driverChatId);
    }

    /**
     * Enable driver to get notifications about passengers' trips
     * @param driverChatId Driver-Bot chat id
     */
    public void subscribeDriverOnUpdate(long driverChatId) {
        driverViewUpdateService.addDriver(driverChatId);
    }

    /**
     * Stop driver from getting notifications about passengers' trips
     * @param driverChatId Driver-Bot chat id
     */
    public void unsubscribeDriverFromUpdate(long driverChatId) {
        driverViewUpdateService.removeDriver(driverChatId);
    }

    public List<Long> getDrivers() {
        return driverViewUpdateService.getAllActiveDriversIds();
    }

    public List<Long> getDriversToUpdate() {
        return driverViewUpdateService.getDriversToUpdate();
    }
}
