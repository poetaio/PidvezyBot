package bots;

import models.dao.DriverUpdateDao;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.driver_services.DriverViewUpdateService;

import java.util.Date;

/**
 * Sort of a task to send notification to all drivers
 */
public class DriverNotificationTask implements Runnable {
    private final String notificationMessage;
    private final String defaultMessage;
    private final Date dateOfNotificationMessage;

    /**
     * Creates an object containing a new task to send a notification to drivers
     * and reset default "broadcast" message
     * Default "broadcast" message is sent at all times except when notification message is sent
     * @param notificationMessage Message to send when time of broadcast hits
     * @param defaultMessage Message to send after a notification message is sent
     * @param dateOfBroadcast date when notification message is sent
     */
    public DriverNotificationTask(String notificationMessage, String defaultMessage, Date dateOfBroadcast) {
        this.notificationMessage = notificationMessage;
        this.defaultMessage = defaultMessage;
        this.dateOfNotificationMessage = dateOfBroadcast;
    }

    @Override
    public void run() {
//        try {
//            thread:
//            while (true) {
//                Date currentDate = new Date();
//                if (dateOfNotificationMessage.before(currentDate)) {
//                    // todo: remove
//                    for (DriverUpdateDao driver : DriverViewUpdateService.getInstance().getAll()) {
//                        ResponseHandler.getInstance(null).sendNotificationToDrivers(driver.getChatId(), notificationMessage, defaultMessage);
//                        break thread;
//                    }
//                }
//                Thread.sleep(10000);
//            }
//        } catch (InterruptedException | TelegramApiException e) {
//            e.printStackTrace();
//        }
    }
}
