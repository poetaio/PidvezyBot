package services.driver_services;

import services.trip_services.TripQueueService;

import java.util.*;

/**
 * Service to manage all users who have hit "Я волонтер"
 */
public class DriverService {
    // singleton
//    private final static DriverService INSTANCE = new DriverService();
//    public static DriverService getInstance() {
//    return INSTANCE;
//}

    private final List<Long> driverList;

    private final DriverViewUpdateService driverViewUpdateService;

    public DriverService() {
        driverList = new LinkedList<>();
        driverViewUpdateService = DriverViewUpdateService.getInstance();
//        driverUpdateMessageService = UpdateMessageService.getInstance();
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
}
