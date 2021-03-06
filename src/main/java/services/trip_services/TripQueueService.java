package services.trip_services;

import models.QueueTrip;
import services.trip_services.utils.TripComparator;
import repositories.TripRepository;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// TODO: rename to TripQueueService
/**
 * Service that contains trip application queue for drivers to view.
 * Details:
 * When passenger approves trip and makes application, it is added to queue of all applications
 * which then are viewed(driver id is set) and
 * accepted - driver id remains non-null, until user hits "I found a car" and the whole trip is removed
 * or skipped - stays in queue, driver id -> null
 */
public class TripQueueService {
    private static TripQueueService instance;
    private final Queue<QueueTrip> tripQueue;
    private final TripRepository tripRepository;

    public TripQueueService(Queue<QueueTrip> tripQueue) {
        this.tripQueue = tripQueue;
        if (instance == null)
            instance = this;
        tripRepository = new TripRepository();
    }

    public static TripQueueService getInstance() {
        return instance;
    }

    /**
     * Get passenger not viewed by anyone (driver id is null),
     * based on current passenger viewed by driver (if any exist)
     * @param driverChatId Id of the driver to show passenger trip application to
     * @return QueuePassengerDao of the first passenger not viewed by anyone
     */
    public QueueTrip getNextFree(long driverChatId) {
        if (tripQueue.isEmpty())
            return null;

        // find current trip by driver id,
        QueueTrip currentViewedTrip = getPassengerDaoByDriverInner(driverChatId);

        if (currentViewedTrip != null) {
            // remove driver from list
            currentViewedTrip.removeDriverChatId(driverChatId);
            UUID currentTripId = currentViewedTrip.getTripId();
            CompletableFuture.runAsync(() ->
                    tripRepository.removeDriverViewFromTrip(currentTripId,
                            driverChatId));
            // reinsert current trip
            tripQueue.remove(currentViewedTrip);
            tripQueue.add(currentViewedTrip);
        }

        // get first
        QueueTrip nextTripToView = tripQueue.poll();
        // if get the same one
        if (nextTripToView == currentViewedTrip && !tripQueue.isEmpty()) {
            nextTripToView = tripQueue.poll();
            tripQueue.add(currentViewedTrip);
        }

        // add driver to list
        nextTripToView.addDriverChatId(driverChatId);
        UUID nextTripId = nextTripToView.getTripId();
        CompletableFuture.runAsync(() ->
                tripRepository.setDriverViewOnTrip(nextTripId,
                        driverChatId));

        // add first to queue
        tripQueue.add(nextTripToView);

        return nextTripToView;
    }

    public QueueTrip getPassengerDaoByDriver(long driverChatId) {
        Optional<QueueTrip> resPassengerDao = tripQueue.stream()
                .filter(trip -> trip.getDriverList() != null
                        && trip.isViewedByDriver(driverChatId))
                .findFirst();
        return resPassengerDao.isEmpty() ? null : resPassengerDao.get().clone();
    }

    public QueueTrip getPassengerDaoByPassenger(long passengerChatId) {
        Optional<QueueTrip> resPassengerDao = tripQueue.stream()
                .filter(trip -> trip.getPassengerChatId() == passengerChatId)
                .findFirst();
        return resPassengerDao.isEmpty() ? null : resPassengerDao.get().clone();
    }

    private QueueTrip getPassengerDaoByDriverInner(long driverChatId) {
        Optional<QueueTrip> resPassengerDao = tripQueue.stream()
                .filter(trip -> trip.getDriverList() != null
                        && trip.isViewedByDriver(driverChatId))
                .findFirst();
        return resPassengerDao.isEmpty() ? null : resPassengerDao.get();
    }

    /**
     * Add passenger trip application to "queue" for viewing by drivers
     * @param trip Trip to add to queue
     */
    public void add(QueueTrip trip) {
        tripQueue.add(trip);
        CompletableFuture.runAsync(() -> tripRepository.addTripToQueue(trip.getTripId()));
    }

    public void removeByPassengerId(long passengerChatId) {
        tripQueue.removeIf(trip -> trip.getPassengerChatId() == passengerChatId);
    }

    /**
     * Removes passenger trip application viewed by driver when driver accepts it
     * @param driverChatId Id of the driver, who views trip application
     */
    public void removeByDriverId(long driverChatId) {
        tripQueue.removeIf(trip -> trip.isViewedByDriver(driverChatId));
    }
    
    public QueueTrip getAndRemoveByDriverId(long driverChatId) {
        QueueTrip resTrip = getPassengerDaoByDriver(driverChatId);
        removeByDriverId(driverChatId);
        return resTrip;
    }

    /**
     * Unsets driver id of trip on which it was set, when driver leaves
     * @param driverChatId  Id of the driver, who's been viewing trip application
     */
    public void unsetView(long driverChatId) {
        tripQueue.stream()
                .filter(x -> x.getDriverList() != null && x.getDriverList().contains(driverChatId))
                .findFirst()
                .ifPresent(trip -> {
                    trip.removeDriverChatId(driverChatId);
                    CompletableFuture.runAsync(() -> tripRepository.unsetDriverTookTrip(trip.getTripId(), driverChatId));
                });
    }

    public void removeAll() {
        tripQueue.clear();
    }

    public List<Long> getPassengersInQueue() {
        return tripQueue.stream().map(QueueTrip::getPassengerChatId).collect(Collectors.toList());
    }

    public List<QueueTrip> getAll() {
        return tripQueue.stream().map(QueueTrip::clone).collect(Collectors.toList());
    }

}

