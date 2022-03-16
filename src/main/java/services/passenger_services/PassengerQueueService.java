package services.passenger_services;

import models.Trip;
import services.TestDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service that contains trip application queue for drivers to view.
 * Details:
 * When passenger approves trip and makes application, it is added to queue of all applications
 * which then are viewed(driver id is set) and
 * accepted - driver id remains non-null, until user hits "I found a car" and the whole trip is removed
 * or skipped - stays in queue, driver id -> null
 */
public class PassengerQueueService {
    private static final PassengerQueueService INSTANCE = new PassengerQueueService();
    private List<Trip> passengerQueue;
    private List<Trip> bufferedTrips;

    private PassengerQueueService() {
        passengerQueue = TestDataService.getTestPassengerQueue();
        bufferedTrips = new ArrayList<>();
    }

    public static PassengerQueueService getInstance() {
        return INSTANCE;
    }

    /**
     * Get passenger not viewed by anyone (driver id is null),
     * based on current passenger viewed by driver (if any exist)
     * @param driverChatId Id of the driver to show passenger trip application to
     * @return QueuePassengerDao of the first passenger not viewed by anyone
     */
    public Trip getNextFree(long driverChatId) {
        if (passengerQueue.isEmpty())
            return null;

        // finding passenger viewed/blocked by the driver
        Trip driverPassenger = getPassengerDaoByDriverInner(driverChatId);

        int index;
        int cnt = 1;

        if (driverPassenger == null) {
            index = 0;
            // if no passengers were taken by driver, we must look through all of them, not all - 1
            cnt = 0;
        } else if (passengerQueue.size() == 1) {
            // if driver took the only passenger, no options left
            driverPassenger.setDriverChatId(null);
            return null;
        } else {
            index = (passengerQueue.indexOf(driverPassenger) + 1) % passengerQueue.size();
            // setting driverId to null, unblocking passenger to get access for other drivers
            driverPassenger.setDriverChatId(null);
        }

        while (passengerQueue.get(index).getDriverChatId() != null) {
            // counting and stopping on full cycle
            if (cnt >= passengerQueue.size() - 1)
                return null;

            index = (index + 1) % passengerQueue.size();
            ++cnt;
        }

        // setting driver id to block the passenger and restrain access for other drivers
        Trip returnPassenger = passengerQueue.get(index);
        returnPassenger.setDriverChatId(driverChatId);
        // returning a copy!!
        return returnPassenger.clone();
    }

    public Trip getPassengerDaoByDriver(long driverChatId) {
        Optional<Trip> resPassengerDao = passengerQueue.stream()
                .filter(passenger -> passenger.getDriverChatId() != null
                        && driverChatId == passenger.getDriverChatId())
                .findFirst();
        return resPassengerDao.isEmpty() ? null : resPassengerDao.get().clone();
    }

    private Trip getPassengerDaoByDriverInner(long driverChatId) {
        Optional<Trip> resPassengerDao = passengerQueue.stream()
                .filter(passenger -> passenger.getDriverChatId() != null
                        && driverChatId == passenger.getDriverChatId())
                .findFirst();
        return resPassengerDao.isEmpty() ? null : resPassengerDao.get();
    }

    /**
     * Add passenger trip application to "queue" for viewing by drivers
     * @param passenger Trip to add to queue
     */
    public void add(Trip passenger) {
        passengerQueue.add(passenger);
    }

    public void returnTripFromBuffer(long passengerChatId) {
        try {
            Trip tripToReturn = bufferedTrips.stream()
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
            bufferedTrips = bufferedTrips.stream()
                    .filter(passenger -> passenger.getPassengerChatId() != passengerChatId)
                    .collect(Collectors.toList());

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void removeAndSaveInBufferByPassengerId(long driverChatId) {
        try {
            Trip tripToBuffer = passengerQueue.stream()
                    .filter(passenger -> passenger.getDriverChatId() == driverChatId)
                    .findFirst()
                    .get();

            bufferedTrips.add(tripToBuffer);
            passengerQueue = passengerQueue.stream()
                    .filter(passenger -> passenger.getDriverChatId() != driverChatId)
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void removeByPassengerId(long passengerChatId) {
        passengerQueue = passengerQueue.stream()
                .filter(passenger -> passenger.getPassengerChatId() != passengerChatId)
                .collect(Collectors.toList());
    }

    /**
     * Removes passenger trip application viewed by driver when driver accepts it
     * @param driverId Id of the driver, who views trip application
     */
    public void removeByDriverId(long driverId) {
        passengerQueue = passengerQueue.stream()
                .filter(passenger -> passenger.getDriverChatId() == null || passenger.getDriverChatId() != driverId)
                .collect(Collectors.toList());
    }

    /**
     * Unsets driver id of trip on which it was set, when driver leaves
     * @param driverChatId  Id of the driver, who's been viewing trip application
     */
    public void unsetView(long driverChatId) {
        passengerQueue.stream()
                .filter(x -> x.getDriverChatId() != null && x.getPassengerChatId() == driverChatId)
                .findFirst()
                .ifPresent(queuePassengerDao -> queuePassengerDao.setDriverChatId(null));
    }

    public void removeAll() {
        passengerQueue = new ArrayList<>();
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
