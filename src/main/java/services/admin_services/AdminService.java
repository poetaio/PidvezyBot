package services.admin_services;

import models.*;
import models.dao.adminDao.AdminInactiveTrip;
import models.dao.adminDao.AdminQueueTrip;
import models.dao.adminDao.AdminTakenTrip;
import models.dao.adminDao.UserDao;
import models.hibernate.Group;
import models.hibernate.utils.GroupCriteria;
import models.utils.GroupStatus;
import org.telegram.telegrambots.meta.api.objects.User;
import repositories.utils.CountGroupDao;
import services.GroupService;
import services.UserService;
import services.driver_services.DriverService;
import services.trip_services.TripService;

import java.util.List;
import java.util.stream.Collectors;

public class AdminService {
    private final UserService userService;
    private final DriverService driverService;
    private final TripService tripService;

    private static final AdminService INSTANCE = new AdminService();

    private AdminService() {
        userService = UserService.getInstance();
        driverService = DriverService.getInstance();
        tripService = TripService.getInstance();
    }

    public static AdminService getInstance() {
        return INSTANCE;
    }

    public List<AdminInactiveTrip> getInactiveTrips() {
        return tripService.getAllNotFinishedTrips()
                .stream()
                .filter(x -> tripService.getTripFromQueueByPassenger(x.getPassengerChatId()) == null)
                .map(this::toAdminInactiveTrip)
                .collect(Collectors.toList());
    }

    public List<AdminQueueTrip> getTripInQueue() {
        return tripService.getAllQueueTrips()
                .stream()
                .map(this::toAdminQueueTrip)
                .collect(Collectors.toList());
    }

    public List<AdminTakenTrip> getTakenTrips() {
        return tripService.getAllTakenTrips()
                .stream()
                .map(this::toAdminTakenTrip)
                .collect(Collectors.toList());
    }

    public List<AdminTakenTrip> getFinishedTrips() {
        return tripService.getFinishedTrips()
                .stream()
                .map(this::toAdminTakenTrip)
                .collect(Collectors.toList());
    }

    private UserDao getUserDaoByChatId(long chatId) {
        User user = userService.getUserInfo(chatId);
        if (user == null) {
            return new UserDao(chatId, null, 0, null, null);
        }
        return new UserDao(chatId, userService.getState(chatId), user.getId(),
                user.getFirstName(), user.getUserName());
    }

    private AdminInactiveTrip toAdminInactiveTrip(QueueTrip trip) {
        UserDao passengerDao = getUserDaoByChatId(trip.getPassengerChatId());
        return new AdminInactiveTrip(passengerDao, trip.getAddress(), trip.getDetails());
    }

    private AdminQueueTrip toAdminQueueTrip(QueueTrip trip) {
        List<UserDao> driverList = trip.getDriverList()
                .stream()
                .map(this::getUserDaoByChatId)
                .collect(Collectors.toList());
        UserDao passengerDao = getUserDaoByChatId(trip.getPassengerChatId());
        return new AdminQueueTrip(passengerDao, trip.getAddress(), trip.getDetails(), driverList);
    }

    private AdminTakenTrip toAdminTakenTrip(TakenTrip trip) {
        UserDao passenger = getUserDaoByChatId(trip.getPassengerChatId());
        UserDao driver = getUserDaoByChatId(trip.getDriverChatId());
        return new AdminTakenTrip(passenger, trip.getAddress(), trip.getDetails(), driver);
    }

    public List<UserDao> getActiveDrivers() {
        return driverService.getDrivers()
                .stream()
                .map(this::getUserDaoByChatId)
                .collect(Collectors.toList());
    }

    public CountGroupDao getGroups(Integer page, Integer limit, GroupCriteria groupCriteria) {
        return GroupService.getInstance().getAllGroups(page, limit, groupCriteria);
    }

    // set status inactive to present group
    public void deactivateGroupFromGroupBot(long chatId) {
        GroupService.getInstance().setGroupInactive(chatId);
    }

    // set status active to present group
    public void activateGroupInGroupBot(long chatId) {
        GroupService.getInstance().setGroupActive(chatId);
    }

    // add new group to group bot if it exists resets the name
    public void addGroupToGroupBot(long chatId, String groupName) {
        GroupService.getInstance().addNewGroup(chatId, groupName);
    }

    // remove totally, if group wants to rejoin it will be able to
    public void removeGroupFromGroupBot(long chatId) {
        GroupService.getInstance().removeGroupIfActive(chatId);
    }
}
