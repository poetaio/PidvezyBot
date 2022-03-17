package services.trip_services;

import models.QueueTrip;
import models.TakenTrip;
import models.hibernate_not_being_used.Trip;

/**
 * Common service to manage trips (delegation)
 * TODO: use
 */
public class TripService {

    private final TripQueueService tripQueueService;
    private final FinishedTripService finishedTripService;
    private final TakenTripService takenTripService;
    private final TripBuilderService tripBuilderService;

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

    public String getTripDetails(long passengerChatId) {
        return tripBuilderService.getDetails(passengerChatId);
    }

    public void addTripToQueue(QueueTrip trip) {
        tripQueueService.add(trip);
    }

    public void removeTripFromQueueByPassengerId(long passengerChatId) {
        tripQueueService.removeByPassengerId(passengerChatId);
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
//        QueueTrip trip = tripQueueService.getAndRemoveByDriverId(driverChatId);
        QueueTrip trip = tripQueueService.getAndRemoveByPassengerId(passengerId);
        TakenTrip takenTrip = new TakenTrip(trip, trip.getDriverList().get(0));
        takenTripService.addTakenTrip(takenTrip);
    }

    // TODO change this piece of code

    /**
     * When driver doesn't like the passenger and hits "Dismiss"
//     * @param driverChatId driver chat id
     */
    public void dismissTrip(long passengerChatId) {
        TakenTrip takenTrip = takenTripService.getTripByPassengerChatId(passengerChatId);
        tripQueueService.add(new QueueTrip(takenTrip));
    }

    /**
     * Make trip status "TAKEN" to start a wonderful journey to destination address
     * @param driverChatId driver chat id
     */
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
}
