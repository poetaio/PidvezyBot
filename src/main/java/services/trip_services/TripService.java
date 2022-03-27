package services.trip_services;

import models.QueueTrip;
import models.TakenTrip;
import models.dao.SendTripDao;
import repositories.TripRepository;
import services.event_service.EventService;
import services.event_service.utils.Events;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Common service to manage trips (delegation)
 * TODO: use
 */
public class TripService {

    private final TripBuilderService tripBuilderService;
    private final TripQueueService tripQueueService;
    private final TakenTripService takenTripService;
    private final FinishedTripService finishedTripService;
    private final TripRepository tripRepository;

    private static TripService INSTANCE;

    public static TripService getInstance() {
        if (INSTANCE == null)
            throw new RuntimeException("Instance has not been initialized");
        return INSTANCE;
    }

    private TripService(Map<Long, QueueTrip> inactiveTrips, Queue<QueueTrip> queueTrips,
                       List<TakenTrip> takenTrips, List<TakenTrip> finishedTrips) {
        tripBuilderService = new TripBuilderService(inactiveTrips);
        tripQueueService = new TripQueueService(queueTrips);
        finishedTripService = new FinishedTripService(finishedTrips);
        takenTripService = new TakenTripService(takenTrips);
        tripRepository = new TripRepository();
    }

    public static void initializeInstance(Map<Long, QueueTrip> builtTrips, Queue<QueueTrip> queueTrips, List<TakenTrip> takenTrips, List<TakenTrip> finishedTrips) {
        if (INSTANCE != null)
            throw new RuntimeException("Instance already initialized");
        INSTANCE = new TripService(builtTrips, queueTrips, takenTrips, finishedTrips);
    }

    public UUID getTripId(long passengerChatId) {
        return tripBuilderService.getTripId(passengerChatId);
    }

    // build trip methods
    public void setTripAddress(long passengerChatId, String address) {
        tripBuilderService.setTripAddress(passengerChatId, address);
    }

    public String getTripAddress(long passengerChatId) {
        return tripBuilderService.getTripAddress(passengerChatId);
    }

    public void setTripDetails(long passengerChatId, String details) {
        tripBuilderService.setTripDetails(passengerChatId, details);
    }

    public String getTripDetails(long passengerChatId) {
        return tripBuilderService.getTripDetails(passengerChatId);
    }

    // trip queue methods
    public void addNewTripToQueue(long chatId) {
        QueueTrip trip = tripBuilderService.getTripInfo(chatId);
        EventService.getInstance().notify(Events.NEW_TRIP_EVENT, new SendTripDao(trip));
        tripQueueService.add(trip);
    }

    public void cancelTripOnSearchStopped(long passengerChatId) {
        QueueTrip trip = tripBuilderService.getTripInfo(passengerChatId);
        tripBuilderService.removeTripInfo(passengerChatId);
        if (trip != null)
            CompletableFuture.runAsync(() -> tripRepository.cancelTrip(trip.getTripId()));
    }

    public List<QueueTrip> getAllNotFinishedTrips() {
        return tripBuilderService.getAll();
    }

    public List<QueueTrip> getAllQueueTrips() {
        return tripQueueService.getAll();
    }

    public List<TakenTrip> getAllTakenTrips() {
        return takenTripService.getAll();
    }

    public List<TakenTrip> getFinishedTrips() {
        return finishedTripService.getAll();
    }

    public QueueTrip getTripFromQueueByDriver(long driverChatId) {
        return tripQueueService.getPassengerDaoByDriver(driverChatId);
    }

    public QueueTrip getTripFromQueueByPassenger(long passengerChatId) {
        return tripQueueService.getPassengerDaoByPassenger(passengerChatId);
    }

    public void removeTripFromQueueByPassengerId(long passengerChatId) {
        tripQueueService.removeByPassengerId(passengerChatId);
        QueueTrip trip = tripBuilderService.getTripInfo(passengerChatId);
        if (trip != null) {
            UUID tripId = trip.getTripId();
            EventService.getInstance().notify(Events.REMOVE_TRIP_EVENT, new SendTripDao(trip));
            CompletableFuture.runAsync(() -> tripRepository.deactivateTrip(tripId));
        }
    }

//    public void removeTripFromQueueByDriverId(long driverChatId) {
//        tripQueueService.removeByDriverId(driverChatId);
//    }

    public QueueTrip findNextTripForDriver(long driverChatId) {
        return tripQueueService.getNextFree(driverChatId);
    }

    public void removeTripOnPassengerFoundACar(long passengerChatId) {
        TakenTrip trip = takenTripService.getAndRemoveTripByPassengerChatId(passengerChatId);
        if (trip != null)
            finishedTripService.addFinishedTrip(trip);
        // if driver takes trip, then refuses, passenger can still finish trip,
        // but the trip will already be in INACTIVE status (after reload thus only in tripBuilderService)
        else {
            QueueTrip inactiveTrip = tripBuilderService.getTripInfo(passengerChatId);
            if (inactiveTrip != null) {
                finishedTripService.addFinishedTrip(inactiveTrip);
            }
        }
        tripQueueService.removeByPassengerId(passengerChatId);
        tripBuilderService.removeTripInfo(passengerChatId);
    }

    public void takeDriverTrip(long driverId) {
        QueueTrip trip = tripQueueService.getAndRemoveByDriverId(driverId);
        if (!trip.getDriverList().contains(driverId))
            throw new RuntimeException("Driver is not viewing trip");
        TakenTrip takenTrip = new TakenTrip(trip, driverId);
        takenTripService.addTakenTrip(takenTrip);
        EventService.getInstance().notify(Events.REMOVE_TRIP_EVENT, new SendTripDao(trip));
        CompletableFuture.runAsync(() -> tripRepository.setDriverTookTrip(trip.getTripId(), driverId));
    }

    /**
     * When driver doesn't like the passenger and hits "Dismiss"
     * @param driverChatId driver chat id
     */
    public void dismissDriverTrip(long driverChatId) {
        takenTripService.dismissDriverTrip(driverChatId);
//        // if not already dismissed by user
//        if (takenTrip != null)
//            tripQueueService.add(new QueueTrip(takenTrip));
    }

    // when passenger does not like driver
    public void dismissPassengerTrip(long passengerChatId) {
        TakenTrip takenTrip = takenTripService.getAndRemoveTripByPassengerChatId(passengerChatId);
        if (takenTrip != null) {
            EventService.getInstance().notify(Events.NEW_TRIP_EVENT, new SendTripDao(takenTrip));
            tripQueueService.add(new QueueTrip(takenTrip));
        }
    }

    /**
     * Get trip which driver has taken
     * @param driverChatId driver chat id
     * @return trip which driver has taken
     */
    public TakenTrip getTakenTripByDriver(long driverChatId) {
        return takenTripService.getTripByDriverChatId(driverChatId);
    }

    /**
     * Get passenger trip
     * @param passengerChatId passenger chat id
     * @return passenger trip
     */
    public TakenTrip getTakenTripByPassenger(long passengerChatId) {
        return takenTripService.getTripByPassengerChatId(passengerChatId);
    }

//    public void finishTripByDriver(long driverChatId) {}

    public void finishTripByPassenger(long passengerChatId) {
        TakenTrip trip = takenTripService.getAndRemoveTripByPassengerChatId(passengerChatId);
        finishedTripService.addFinishedTrip(trip);
    }

    public List<Long> getPassengersInQueue() {
        return tripQueueService.getPassengersInQueue();
    }

    public TripBuilderService getTripBuilderService() {
        return tripBuilderService;
    }

    public TakenTripService getTakenTripService() {
        return takenTripService;
    }
}
