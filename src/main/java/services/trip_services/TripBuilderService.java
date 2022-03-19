package services.trip_services;

import models.QueueTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service that builds and stores information about user trip application
 * (address, details, driver who views & takes the trip)
 */
public class TripBuilderService {
    private final Map<Long, QueueTrip> tripInfoMap;

    public TripBuilderService() {
        tripInfoMap = new HashMap<>();
    }
    /**
     * Saving user info to database
     * @param passengerChatId User-Bot chat id
     * @param passengerInfo QueuePassengerDao entity, contains user chat id, address, details, driver chat id
     */
    public void putPassengerInfo(long passengerChatId, QueueTrip passengerInfo) {
        tripInfoMap.put(passengerChatId, passengerInfo);
    }

    public QueueTrip getTripInfo(long passengerUserId) {
        return tripInfoMap.get(passengerUserId).clone();
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
        tripInfoMap.remove(chatId);
    }

    private QueueTrip getTripInfoWithDefault(long chatId) {
        QueueTrip passengerTripInfo = tripInfoMap.get(chatId);
        if (passengerTripInfo == null) {
            passengerTripInfo = new QueueTrip();
            passengerTripInfo.setPassengerChatId(chatId);
            tripInfoMap.put(chatId, passengerTripInfo);
        }
        return passengerTripInfo;
    }

    public List<QueueTrip> getAll() {
        return tripInfoMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
    }
}
