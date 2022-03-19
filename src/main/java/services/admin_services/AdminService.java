package services.admin_services;

import lombok.AllArgsConstructor;
import models.*;
import models.dao.UserDao;
import org.telegram.telegrambots.meta.api.objects.User;
import services.UserService;
import services.driver_services.DriverService;
import services.passenger_services.NumberService;
import services.trip_services.TripService;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class AdminService {
    private final UserService userService;
    private final DriverService driverService;
    private final NumberService numberService;
    private final TripService tripService;

    public List<AdminInactiveTrip> getInactiveTrips() {
        return tripService.getAllTrips()
                .stream()
                .filter(x -> tripService.getTripFromQueueByPassenger(x.getPassengerChatId()) == null)
                .map(this::toAdminInactiveTrip)
                .collect(Collectors.toList());
    }

    public List<AdminQueueTrip> getTripInQueue() {
        return tripService.getAllTripsFromQueue()
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
}
