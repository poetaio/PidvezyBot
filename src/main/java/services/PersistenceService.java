package services;

import bots.utils.Constants;
import models.QueueTrip;
import models.TakenTrip;
import models.dao.DriverUpdateDao;
import models.hibernate.Group;
import models.hibernate.GroupMessage;
import models.hibernate.Trip;
import models.hibernate.User;
import models.utils.GroupStatus;
import models.utils.State;
import services.event_service.EventService;
import services.trip_services.utils.TripComparator;
import org.hibernate.Session;
import services.driver_services.DriverService;
import services.trip_services.TripService;
import utils.HibernateUtil;

import java.util.*;

// make me persistent baby
public class PersistenceService {
    public static void initServices() {
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

        Collection<Long> inactiveGroupIds = new HashSet<>();
        Collection<Long> activeGroupIds = new HashSet<>();
        Map<Long, String> groupNamesMap = new HashMap<>();
        Map<Long, Map<UUID, Integer>> groupTripMessageMap = new HashMap<>();

        List<Group> allGroups = session.createQuery("SELECT g FROM groups g", Group.class).getResultList();
        allGroups.forEach(x -> {
            groupNamesMap.put(x.getGroupId(), x.getGroupName());
            if (x.getGroupStatus() == GroupStatus.ACTIVE)
                activeGroupIds.add(x.getGroupId());
            else
                inactiveGroupIds.add(x.getGroupId());
            for (GroupMessage groupMessage : x.getGroupMessages())
                groupTripMessageMap.computeIfAbsent(x.getGroupId(), y -> new HashMap<>())
                        .put(groupMessage.getTrip().getTripId(), groupMessage.getMessageId());
        });

        EventService.initializeInstance();
        DriverService.initializeInstance(driverList, driverUpdateDaos);
        TripService.initializeInstance(builtTrips, queueTrips, takenTrips, finishedTrips);
        UserService.initializeInstance(userStatesMap, userInfo, userNumbers);
        GroupService.initializeInstance(activeGroupIds, inactiveGroupIds, groupNamesMap, groupTripMessageMap);

        session.getTransaction().commit();
    }

    private static org.telegram.telegrambots.meta.api.objects.User toTelegramUser(User user) {
        if (user.getFirstName() == null)
            return null;
        return new org.telegram.telegrambots.meta.api.objects.User(user.getUserId(), user.getFirstName(),
                false, user.getLastName(), user.getUsername(), "", false, false,
                false);
    }
}
