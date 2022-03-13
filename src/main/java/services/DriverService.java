package services;

import org.telegram.telegrambots.meta.api.objects.User;

import java.util.*;

public class DriverService {
    private final static DriverService INSTANCE = new DriverService();
    private final List<Long> driverList;
    private final DriverUpdateService driverUpdateService;
//    private final UpdateMessageService driverUpdateMessageService;

    private DriverService() {
        driverList = new LinkedList<>();
        driverUpdateService = DriverUpdateService.getInstance();
//        driverUpdateMessageService = UpdateMessageService.getInstance();
    }

    public static DriverService getInstance() {
        return INSTANCE;
    }

    public void removeDriver(long driverChatId) {
        driverList.remove(driverChatId);
        driverUpdateService.removeDriver(driverChatId);
        PassengerQueueService.getInstance().unsetView(driverChatId);
    }

    public void addDriver(long driverChatId) {
        if (driverList.contains(driverChatId))
            return;

        driverList.add(driverChatId);
        driverUpdateService.addDriver(driverChatId);
    }

    // driverUpdateService methods
    public void resetDriverTime(long chatId) {
        driverUpdateService.resetDriverTime(chatId);
    }

//    // driverUpdateMessageService methods
//    public void putMessageToUpdateId(long driverChatId, int messageToUpdateId) {
//        driverUpdateMessageService.putMessageToUpdate(driverChatId, messageToUpdateId);
//    }
//
//    public Integer getMessageToUpdateId(long driverChatId) {
//        return driverUpdateMessageService.getMessageToUpdate(driverChatId);
//    }

    // other
    /**
     * Add driver info to the database
     * @param driverCharId Driver-Bot chat id
     * @param driverInfo User entity, contains user id, username, first name, last name
     */
    public void registerDriver(long driverCharId, User driverInfo) {

    }

    /**
     * Enable driver to get notifications about users' trips
     * @param driverChatId Driver-Bot chat id
     */
    public void subscribeDriver(long driverChatId) {

    }

    /**
     * Stop driver from getting notifications about users' trips
     * @param driverChatId Driver-Bot chat id
     */
    public void unsubscribeDriver(long driverChatId) {

    }
}
