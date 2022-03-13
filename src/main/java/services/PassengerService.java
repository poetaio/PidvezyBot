package services;

import models.dao.QueuePassengerDao;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.Map;

public class PassengerService {
    private static final PassengerService INSTANCE = new PassengerService();
    private final Map<Long, QueuePassengerDao> tripInfo;

    public static PassengerService getInstance() {
        return INSTANCE;
    }

    private PassengerService() {
        tripInfo = new HashMap<>();
    }

    /**
     * Saving user info to database
     * @param passengerChatId User-Bot chat id
     * @param passengerInfo QueuePassengerDao entity, contains user chat id, address, details, driver chat id
     */
    public void putPassengerInfo(long passengerChatId, QueuePassengerDao passengerInfo) {
        tripInfo.put(passengerChatId, passengerInfo);
    }

    public QueuePassengerDao getTripInfo(long passengerUserId) {
        return tripInfo.get(passengerUserId).clone();
    }

    /**
     * Creating an active trip by saving passenger address
     * @param passengerUserId User account id
     * @param address Destination address
     */
    public void addAddress(long passengerUserId, String address) {
        getTripInfoWithDefault(passengerUserId).setAddress(address);
    }

    public String getAddress(long passengerChatId) {
        return getTripInfoWithDefault(passengerChatId).getAddress();
    }

    /**
     * Adding details to an active trip by userId
     * @param passengerUserId User account id
     * @param details Details of the trip
     */
    public void addDetails(long passengerUserId, String details) {
        getTripInfoWithDefault(passengerUserId).setDetails(details);
    }

    public String getDetails(long passengerChatId) {
        return getTripInfoWithDefault(passengerChatId).getDetails();
    }

    public void removeTripInfo(long chatId) {
        tripInfo.remove(chatId);
    }

    private QueuePassengerDao getTripInfoWithDefault(long chatId) {
        QueuePassengerDao passengerTripInfo = tripInfo.get(chatId);
        if (passengerTripInfo == null) {
            passengerTripInfo = new QueuePassengerDao();
            passengerTripInfo.setPassengerChatId(chatId);
            tripInfo.put(chatId, passengerTripInfo);
        }
        return passengerTripInfo;
    }
}
