package bots;

import bots.response_handler.ResponseHandler;
import bots.utils.State;
import models.dao.DriverUpdateDao;
import models.dao.QueuePassengerDao;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.DriverUpdateService;
import services.PassengerQueueService;
import services.UpdateMessageService;

import java.util.Date;

/**
 * Service to update current trip application viewed by driver every N seconds
 */
public class BroadcastScheduler implements Runnable {
    private final String message;
    private final String trainTime;
    private final Date dateOfBroadcast;

    public BroadcastScheduler(String message, String trainTime, Date dateOfBroadcast) {
        this.message = message;
        this.trainTime = trainTime;
        this.dateOfBroadcast = dateOfBroadcast;
    }

    @Override
    public void run() {
        try {
            thread:
            while (true) {
                Date currentDate = new Date();
                if (dateOfBroadcast.before(currentDate)) {
                    for (DriverUpdateDao driver : DriverUpdateService.getInstance().getAll()) {
                        ResponseHandler.getInstance(null).setCurrentScheduleMessage(message, trainTime);
                        ResponseHandler.getInstance(null).sendScheduleMessage(driver.getChatId(), true);
                        break thread;
                    }
                }
                Thread.sleep(10000);
            }
        } catch (InterruptedException | TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
