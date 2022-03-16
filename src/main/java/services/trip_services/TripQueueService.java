package services.trip_services;

import models.QueueTrip;
import models.utils.TripComparator;
import services.TestDataService;

import java.util.*;
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
    private static final TripQueueService INSTANCE = new TripQueueService();
    // pq goes in here, yeah boiiii
    private Queue<QueueTrip> tripQueue;
    private Queue<QueueTrip> bufferedTrips;

    private TripQueueService() {
        tripQueue = TestDataService.getTestPassengerQueue();
        bufferedTrips = new PriorityQueue<>(TripComparator.TRIP_COMPARATOR);
    }

    public static TripQueueService getInstance() {
        return INSTANCE;
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
        assert nextTripToView != null;
        nextTripToView.addDriverChatId(driverChatId);

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
    }

    public void returnTripFromBuffer(long passengerChatId) {
        try {
            QueueTrip tripToReturn = bufferedTrips.stream()
                    .filter(trip -> trip.getPassengerChatId() == passengerChatId)
                    .findFirst()
                    .get();

            bufferedTrips.remove(tripToReturn);
            add(tripToReturn);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void removeTripFromBuffer(long passengerChatId) {
        try {
            bufferedTrips.removeIf(passenger -> passenger.getPassengerChatId() != passengerChatId);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void removeAndSaveInBufferByPassengerId(long passengerChatId) {
        try {
            QueueTrip tripToBuffer = tripQueue.stream()
                    .filter(trip -> trip.getPassengerChatId() == passengerChatId)
                    .findFirst()
                    .get();

            bufferedTrips.add(tripToBuffer);
            tripQueue.removeIf(trip -> trip.getPassengerChatId() != passengerChatId);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
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
                .filter(x -> x.getDriverList() != null && x.getPassengerChatId() == driverChatId)
                .findFirst()
                .ifPresent(queuePassengerDao -> queuePassengerDao.removeDriverChatId(driverChatId));
    }

    public void removeAll() {
        tripQueue.clear();
    }

//    // testing queue service
//    public static void main(String[] args) {
//        PassengerQueueService pqservice = new PassengerQueueService();
//        Trip ps2 = new Trip(2l, "Lavruhina", "A child, a mom", 7l);
//        Trip ps3 = new Trip(3l, "Lavruhina", "A child, a mom", 6l);
//        Trip ps4 = new Trip(4l, "Lavruhina", "A child, a mom", 5l);
//        Trip ps5 = new Trip(5l,"Lavruhina", "A child, a mom", 3l);
//        Trip ps6 = new Trip(6l,"Lavruhina", "A child, a mom", 4l);
//
//        pqservice.add(ps2);
//        pqservice.add(ps3);
//        pqservice.add(ps4);
//        pqservice.add(ps5);
//        pqservice.add(ps6);
//
//        System.out.println(pqservice.getNextFree(2)); // all passengers are being viewed
//        System.out.println(pqservice.getNextFree(3)); // frees passenger 5
//        System.out.println(pqservice.getNextFree(5)); // gets passenger 5
//
//        System.out.println("___");
//        PassengerQueueService pq = new PassengerQueueService();
//        pq.add(ps2);
//        System.out.println(pq.getNextFree(7));
//    }
}

