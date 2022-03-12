package services;

import models.dao.QueuePassengerDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * When passenger approves trip and makes application, it is added to queue of all application
 * which are currently being viewed by drivers and accepted or skipped
 */
public class PassengerQueueService {
    private static final PassengerQueueService INSTANCE = new PassengerQueueService();
    private List<QueuePassengerDao> passengerQueue;

    private PassengerQueueService() {
        passengerQueue = new ArrayList<>();
    }

    public static PassengerQueueService getInstance() {
        return INSTANCE;
    }

    /**
     * Get passenger not viewed by anyone, based on current passenger viewed by driver (if any exist)
     * @param driverChatId Id of the driver to show passenger trip application to
     * @return QueuePassengerDao of the first passenger not viewed by anyone
     */
    public QueuePassengerDao getNextFree(long driverChatId) {
        if (passengerQueue.isEmpty())
            return null;

        // finding passenger viewed/blocked by the driver
        QueuePassengerDao driverPassenger = getPassengerDaoByDriverInner(driverChatId);

        int index;
        if (driverPassenger == null) {
            index = 0;
        } else if (passengerQueue.size() == 1) {
            // if driver took the only passenger, no options left
            driverPassenger.setDriverChatId(null);
            return null;
        } else {
            index = (passengerQueue.indexOf(driverPassenger) + 1) % passengerQueue.size();
            // setting driverId to null, unblocking passenger to get access for other drivers
            driverPassenger.setDriverChatId(null);
        }

        int cnt = 1;
        while (passengerQueue.get(index).getDriverChatId() != null) {
            // counting and stopping on full cycle
            if (cnt >= passengerQueue.size() - 1)
                return null;

            index = index + 1 == passengerQueue.size() ? 0 : index + 1;
            ++cnt;
        }

        // setting driver id to block the passenger and restrain access for other drivers
        QueuePassengerDao returnPassenger = passengerQueue.get(index);
        returnPassenger.setDriverChatId(driverChatId);
        // returning a copy!!
        return returnPassenger.clone();
    }

    public QueuePassengerDao getPassengerDaoByDriver(long driverChatId) {
        Optional<QueuePassengerDao> resPassengerDao = passengerQueue.stream()
                .filter(passenger -> passenger.getDriverChatId() != null
                        && driverChatId == passenger.getDriverChatId())
                .findFirst();
        return resPassengerDao.isEmpty() ? null : resPassengerDao.get().clone();
    }

    private QueuePassengerDao getPassengerDaoByDriverInner(long driverChatId) {
        Optional<QueuePassengerDao> resPassengerDao = passengerQueue.stream()
                .filter(passenger -> passenger.getDriverChatId() != null
                        && driverChatId == passenger.getDriverChatId())
                .findFirst();
        return resPassengerDao.isEmpty() ? null : resPassengerDao.get();
    }

    /**
     * Add passenger trip application to "queue" for viewing by drivers
     * @param passenger Trip to add to queue
     */
    public void add(QueuePassengerDao passenger) {
        passengerQueue.add(passenger);
    }

    /**
     * Removes passenger trip application viewed by driver when driver accepts it
     * @param driverId Id of the driver, who views trip application
     */
    public void remove(long driverId) {
        passengerQueue = passengerQueue.stream()
                .filter(passenger -> passenger.getDriverChatId() != null && passenger.getDriverChatId() != driverId)
                .collect(Collectors.toList());
    }

    public void removeAll() {
        passengerQueue = new ArrayList<>();
    }

    // testing queue service
    public static void main(String[] args) {
        PassengerQueueService pqservice = new PassengerQueueService();
        QueuePassengerDao ps2 = new QueuePassengerDao(2l, "Lavruhina", "A child, a mom", 7l);
        QueuePassengerDao ps3 = new QueuePassengerDao(3l, "Lavruhina", "A child, a mom", 6l);
        QueuePassengerDao ps4 = new QueuePassengerDao(4l, "Lavruhina", "A child, a mom", 5l);
        QueuePassengerDao ps5 = new QueuePassengerDao(5l,"Lavruhina", "A child, a mom", 3l);
        QueuePassengerDao ps6 = new QueuePassengerDao(6l,"Lavruhina", "A child, a mom", 4l);

        pqservice.add(ps2);
        pqservice.add(ps3);
        pqservice.add(ps4);
        pqservice.add(ps5);
        pqservice.add(ps6);

        System.out.println(pqservice.getNextFree(2)); // all passengers are being viewed
        System.out.println(pqservice.getNextFree(3)); // frees passenger 5
        System.out.println(pqservice.getNextFree(5)); // gets passenger 5

        System.out.println("___");
        PassengerQueueService pq = new PassengerQueueService();
        pq.add(ps2);
        System.out.println(pq.getNextFree(7));
    }
}
