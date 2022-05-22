package services;

import bots.utils.Constants;
import models.QueueTrip;
import models.TakenTrip;
import models.dao.DriverUpdateDao;
import models.hibernate.Trip;
import models.hibernate.User;
import models.utils.State;
import models.utils.TripComparator;
import org.hibernate.Session;
import services.driver_services.DriverService;
import services.driver_services.utils.DriverUpdateEvents;
import services.passenger_services.NumberService;
import services.trip_services.TripService;
import utils.HibernateUtil;

import java.util.*;

// make me persistent baby
public class PersistenceService {
    private static DriverService driverService;
    private static NumberService numberService;
    private static UserService userService;
    private static TripService tripService;

    public static DriverService initServices(DriverUpdateEvents driverUpdateEvents) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Map<Long, State> userStatesMap = new HashMap<>();
        Map<Long, String> userNumbers = new HashMap<>();
        Map<Long, org.telegram.telegrambots.meta.api.objects.User> userInfo = new HashMap<>();

        List<Long> driverList = new LinkedList<>();
        List<DriverUpdateDao> driverUpdateDaos = new LinkedList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);

        List<User> allUsers = session.createQuery("SELECT u FROM users u", User.class).getResultList();

        for (User user : allUsers) {
//            System.out.printf("%d %s %s%n", user.getUserId(), user.getFirstName(), user.getUsername());
            userNumbers.put(user.getUserId(), user.getPhoneNumber());
            userStatesMap.put(user.getUserId(), user.getUserState());
            userInfo.put(user.getUserId(), toTelegramUser(user));

            // drivers queue
            if (user.getUserState() == State.DRIVER_ACTIVE ||
                    user.getUserState() == State.NO_TRIPS_AVAILABLE) {
                driverList.add(user.getUserId());
                driverUpdateDaos.add(new DriverUpdateDao(user.getUserId(), calendar.getTime()));
            }
        }
        // (all not finished trips) building trips or (inactive + in queue) not sure
        // trips queue
        // taken trips
        // finished trips

        Map<Long, QueueTrip> builtTrips = new HashMap<>();
        Queue<QueueTrip> queueTrips = new PriorityQueue<>(TripComparator.TRIP_COMPARATOR);
        List<TakenTrip> takenTrips = new LinkedList<>();
        List<TakenTrip> finishedTrips = new LinkedList<>();

        List<Trip> allTrips = session.createQuery("Select t from trips t", Trip.class).getResultList();

        for (Trip trip : allTrips) {
            switch (trip.getTripStatus()) {
                case INACTIVE:
                    builtTrips.put(trip.getPassenger().getUserId(),
                            new QueueTrip(trip.getTripId(), trip.getPassenger().getUserId(),
                            trip.getAddress(), trip.getDetails()));
                    break;
                case IN_QUEUE:
                    QueueTrip newQueueTrip = new QueueTrip(trip.getTripId(), trip.getPassenger().getUserId(),
                            trip.getAddress(), trip.getDetails());
                    queueTrips.add(newQueueTrip);
                    builtTrips.put(trip.getPassenger().getUserId(), newQueueTrip);
                    break;
                case TAKEN:
                    takenTrips.add(new TakenTrip(trip.getTripId(), trip.getPassenger().getUserId(),
                            trip.getAddress(), trip.getDetails(), trip.getTakenByDriver().getUserId()));
                    builtTrips.put(trip.getPassenger().getUserId(),
                            new QueueTrip(trip.getTripId(), trip.getPassenger().getUserId(),
                                    trip.getAddress(), trip.getDetails()));
                    break;
                case FINISHED:
                    User driver = trip.getFinishedByDriver();
                    finishedTrips.add(new TakenTrip(trip.getTripId(), trip.getPassenger().getUserId(),
                            trip.getAddress(), trip.getDetails(), driver != null ? driver.getUserId() : null));
                    break;
            }
        }

        PersistenceService.driverService = new DriverService(driverList, driverUpdateDaos, driverUpdateEvents);
        numberService = new NumberService(userNumbers);
        tripService = new TripService(builtTrips, queueTrips, takenTrips, finishedTrips);
        userService = new UserService(userStatesMap, userInfo, PersistenceService.driverService, tripService);


        session.getTransaction().commit();

        return driverService;
    }

    private static org.telegram.telegrambots.meta.api.objects.User toTelegramUser(User user) {
        if (user.getFirstName() == null)
            return null;
        return new org.telegram.telegrambots.meta.api.objects.User(user.getUserId(), user.getFirstName(),
                false, user.getLastName(), user.getUsername(), "", false, false,
                false);
    }

    public static DriverService getDriverService() {
        return driverService;
    }

    public static NumberService getNumberService() {
        return numberService;
    }

    public static UserService getUserService() {
        return userService;
    }

    public static TripService getTripService() {
        return tripService;
    }
}
