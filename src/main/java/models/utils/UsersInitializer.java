package models.utils;

import bots.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.AdminInactiveTrip;
import models.QueueTrip;
import models.dao.DriverUpdateDao;
import models.dao.UserDao;
import org.telegram.telegrambots.meta.api.objects.User;
import services.UserService;
import services.driver_services.DriverService;
import services.trip_services.TripBuilderService;
import services.trip_services.TripService;

import java.util.Calendar;

public class UsersInitializer {

    private UsersInitializer() {
    }

    public static void parseDrivers(DriverService driverService, UserService userService) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String driversStringInput = System.getenv("DRIVERS_TO_ADD");
        if (driversStringInput != null) {
            UserDao[] userDaos = mapper.readValue(driversStringInput, UserDao[].class);
            for (UserDao userDao : userDaos) {
                driverService.getDriversList().add(userDao.getChatId());
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);
                driverService.getDriverUpdateQueueList().add(new DriverUpdateDao(userDao.getChatId(), calendar.getTime()));

                User user = new User();
                user.setId(userDao.getUserId());
                user.setFirstName(userDao.getFirstName());
                user.setUserName(userDao.getUserName());
                userService.putUserInfo(userDao.getChatId(), user);
                userService.putState(userDao.getChatId(), State.NO_TRIPS_AVAILABLE);
            }
        }
    }

    public static void parseInactiveTrips(TripService tripService, TripBuilderService tripBuilderService, UserService userService) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String tripsStringInput = System.getenv("INACTIVE_TRIPS_TO_ADD");
        if (tripsStringInput != null) {
            AdminInactiveTrip[] tripsDao = mapper.readValue(tripsStringInput, AdminInactiveTrip[].class);
            for (AdminInactiveTrip tripDao : tripsDao) {
                UserDao userDao = tripDao.getPassenger();

                User user = new User();
                user.setId(userDao.getUserId());
                user.setFirstName(userDao.getFirstName());
                user.setUserName(userDao.getUserName());
                userService.putUserInfo(userDao.getChatId(), user);
                userService.putState(userDao.getChatId(), userDao.getCurrentState());

                QueueTrip queueTrip = new QueueTrip();
                queueTrip.setPassengerChatId(userDao.getChatId());
                queueTrip.setAddress(tripDao.getAddress());
                queueTrip.setDetails(tripDao.getDetails());

                tripBuilderService.putPassengerInfo(userDao.getChatId(), queueTrip);
                tripService.addNewTrip(user.getId());

            }
        }
    }

}
