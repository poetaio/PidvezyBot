package services.passenger_services;

import models.Trip;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that contains information about user trip application
 * (address, details, driver who views & takes the trip)
 */
public class PassengerService {
    // singleton
    private static final PassengerService INSTANCE = new PassengerService();
    public static PassengerService getInstance() {
        return INSTANCE;
    }

    // chat id - trip
    private final Map<Long, Trip> tripInfo;

    private PassengerService() {
        tripInfo = new HashMap<>();
    }

    /**
     * Saving user info to database
     * @param passengerChatId User-Bot chat id
     * @param passengerInfo QueuePassengerDao entity, contains user chat id, address, details, driver chat id
     */
    public void putPassengerInfo(long passengerChatId, Trip passengerInfo) {
        tripInfo.put(passengerChatId, passengerInfo);
    }

    public Trip getTripInfo(long passengerUserId) {
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

    private Trip getTripInfoWithDefault(long chatId) {
        Trip passengerTripInfo = tripInfo.get(chatId);
        if (passengerTripInfo == null) {
            passengerTripInfo = new Trip();
            passengerTripInfo.setPassengerChatId(chatId);
            tripInfo.put(chatId, passengerTripInfo);
        }
        return passengerTripInfo;
    }
}
