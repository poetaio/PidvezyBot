package services.trip_services;

import models.QueueTrip;
import repositories.TripRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service that builds and stores information about user trip application
 * (address, details, driver who views & takes the trip)
 */
public class TripBuilderService {
    private final Map<Long, QueueTrip> tripInfoMap;
    private final TripRepository tripRepository;

    public TripBuilderService(Map<Long, QueueTrip> tripInfoMap) {
        this.tripInfoMap = tripInfoMap;
        tripRepository = new TripRepository();
    }
    /**
     * Saving user info to database
     * @param passengerChatId User-Bot chat id
     * @param newTrip QueuePassengerDao entity, contains user chat id, address, details, driver chat id
     */
    public void putPassengerInfo(long passengerChatId, QueueTrip newTrip) {
        tripInfoMap.put(passengerChatId, newTrip);
        CompletableFuture.runAsync(() -> tripRepository.createNewTrip(newTrip));
    }

    public QueueTrip getTripInfo(long passengerUserId) {
        QueueTrip trip = tripInfoMap.get(passengerUserId);
        if (trip != null)
            return trip.clone();
        return null;
    }

    /**
     * Creating an active trip by saving passenger address
     * @param passengerUserId User account id
     * @param address Destination address
     */
    public void setTripAddress(long passengerUserId, String address) {
        QueueTrip trip = getTripInfoWithDefault(passengerUserId);
        trip.setAddress(address);
        CompletableFuture.runAsync(() -> tripRepository.setAddress(trip.getTripId(), address));
    }

    public String getTripAddress(long passengerChatId) {
        return getTripInfoWithDefault(passengerChatId).getAddress();
    }

    /**
     * Adding details to an active trip by userId
     * @param passengerUserId User account id
     * @param details Details of the trip
     */
    public void setTripDetails(long passengerUserId, String details) {
        QueueTrip trip = getTripInfoWithDefault(passengerUserId);
        trip.setDetails(details);
        CompletableFuture.runAsync(() -> tripRepository.setDetails(trip.getTripId(), details));
    }

    public String getTripDetails(long passengerChatId) {
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
            // adding trip to db
            UUID tripId = passengerTripInfo.getTripId();
            CompletableFuture.runAsync(() -> tripRepository.initTrip(tripId, chatId));
        }
        return passengerTripInfo;
    }

    public List<QueueTrip> getAll() {
        return tripInfoMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
    }
}
