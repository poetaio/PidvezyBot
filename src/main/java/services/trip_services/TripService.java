package services.trip_services;

import models.QueueTrip;
import models.TakenTrip;

import java.util.Arrays;
import java.util.List;

/**
 * Common service to manage trips (delegation)
 * TODO: use
 */
public class TripService {

    private final TripBuilderService tripBuilderService;
    private final TripQueueService tripQueueService;
    private final TakenTripService takenTripService;
    private final FinishedTripService finishedTripService;

    public TripService() {
        tripQueueService = TripQueueService.getInstance();
        finishedTripService = new FinishedTripService();
        takenTripService = new TakenTripService();
        tripBuilderService = new TripBuilderService();
    }

    /**
     * Finish making trip, approve it
     * @param trip approved trip by driver
     */
    public void makeNewTrip(QueueTrip trip) {
        tripQueueService.add(trip);
    }

    public void addAddressToTrip(long passengerChatId, String address) {
        tripBuilderService.addAddress(passengerChatId, address);
    }

    public String getTripAddress(long passengerChatId) {
        return tripBuilderService.getAddress(passengerChatId);
    }

    public void addDetailsToTrip(long passengerChatId, String details) {
        tripBuilderService.addDetails(passengerChatId, details);
    }

    public void addNewTrip(long chatId) {
        tripQueueService.add(tripBuilderService.getTripInfo(chatId));
    }

    public String getTripDetails(long passengerChatId) {
        return tripBuilderService.getDetails(passengerChatId);
    }

    public void removeTripDetails(long passengerChatId) {
        tripBuilderService.removeTripInfo(passengerChatId);
    }

    public List<QueueTrip> getAllTrips() {
        return tripBuilderService.getAll();
    }

    public void addTripToQueue(QueueTrip trip) {
        tripQueueService.add(trip);
    }

    public QueueTrip getTripFromQueueByDriver(long driverChatId) {
        return tripQueueService.getPassengerDaoByDriver(driverChatId);
    }

    public QueueTrip getTripFromQueueByPassenger(long passengerChatId) {
        return tripQueueService.getPassengerDaoByPassenger(passengerChatId);
    }

    public QueueTrip findNextQueueTrip(long driverChatId) {
        return tripQueueService.getNextFree(driverChatId);
    }

    public void removeTripFromQueueByPassengerId(long passengerChatId) {
        tripQueueService.removeByPassengerId(passengerChatId);
    }

    public List<QueueTrip> getAllTripsFromQueue() {
        return tripQueueService.getAll();
    }

    public void passengerFoundACar(long passengerChatId) {
        tripQueueService.removeByPassengerId(passengerChatId);
        tripBuilderService.removeTripInfo(passengerChatId);
    }

    public void removeTripFromQueueByDriverId(long driverChatId) {
        tripQueueService.removeByDriverId(driverChatId);
    }

    /**
     * Find next trip for driver to view, add to the list of "viewers"
     * @param driverChatId driver chat id
     * @return next trip for the driver to view
     */
    public QueueTrip findNewTripForDriver(long driverChatId) {
        return tripQueueService.getPassengerDaoByDriver(driverChatId);
    }

    /**
     * Assign the driver for the trip, that he's currently viewing.
     * Create taken trip to block trip for other drivers to take
//     * @param driverChatId driver chat id
     */
    public void takeTrip(long passengerId) {
        QueueTrip trip = tripQueueService.getAndRemoveByPassengerId(passengerId);
        TakenTrip takenTrip = new TakenTrip(trip, trip.getDriverList().get(0));
        takenTripService.addTakenTrip(takenTrip);
    }

    public void takeDriverTrip(long driverId) {
        QueueTrip trip = tripQueueService.getAndRemoveByDriverId(driverId);
        TakenTrip takenTrip = new TakenTrip(trip, trip.getDriverList().get(0));
        takenTripService.addTakenTrip(takenTrip);
    }

    /**
     * When driver doesn't like the passenger and hits "Dismiss"
     * @param driverChatId driver chat id
     */
    public void dismissDriverTrip(long driverChatId) {
//        TakenTrip takenTrip = takenTripService.getAndRemoveTripByDriverChatId(driverChatId);
//        // if not already dismissed by user
//        if (takenTrip != null)
//            tripQueueService.add(new QueueTrip(takenTrip));
    }

    // removes from taken when finishing trip by passenger
    public void finishPassengerTrip(long passengerChatId) {
        takenTripService.removePassengerTrip(passengerChatId);
    }

    // when passenger does not like driver
    public void dismissPassengerTrip(long passengerChatId) {
        TakenTrip takenTrip = takenTripService.getAndRemoveTripByPassengerChatId(passengerChatId);
        if (takenTrip != null)
            tripQueueService.add(new QueueTrip(takenTrip));
    }

    public void approveTrip(long driverChatId) {
        takenTripService.approveTrip(driverChatId);
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

    public void finishTripByDriver(long driverChatId) {
        TakenTrip trip = takenTripService.getAndRemoveTripByDriverChatId(driverChatId);
        finishedTripService.addFinishedTrip(trip);
    }

    public void finishTripByPassenger(long passengerChatId) {
        TakenTrip trip = takenTripService.getAndRemoveTripByPassengerChatId(passengerChatId);
        finishedTripService.addFinishedTrip(trip);
    }

    public List<Long> getPassengersInQueue() {
        return tripQueueService.getPassengersInQueue();
    }

    public List<TakenTrip> getAllTakenTrips() {
        return takenTripService.getAll();
    }

    public List<TakenTrip> getFinishedTrips() {
        return finishedTripService.getAll();
    }
}
