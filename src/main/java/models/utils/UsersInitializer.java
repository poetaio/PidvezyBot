package models.utils;

import bots.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.*;
import models.dao.*;
import models.dao.adminDao.AdminInactiveTrip;
import models.dao.adminDao.AdminQueueTrip;
import models.dao.adminDao.AdminTakenTrip;
import models.dao.adminDao.UserDao;
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
            }
        }
    }

    public static void parseQueueTrip(TripService tripService, TripBuilderService tripBuilderService, UserService userService) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String tripsStringInput = System.getenv("QUEUE_TRIPS_TO_ADD");
        if (tripsStringInput != null) {
            AdminQueueTrip[] tripsDao = mapper.readValue(tripsStringInput, AdminQueueTrip[].class);
            for (AdminQueueTrip tripDao : tripsDao) {
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
                tripService.addNewTripToQueue(user.getId());
            }
        }
    }

    public static void parseTakenTrips(TripService tripService, TripBuilderService tripBuilderService, UserService userService) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String tripsStringInput = System.getenv("TAKEN_TRIPS_TO_ADD");
        if (tripsStringInput != null) {
            AdminTakenTrip[] tripsDao = mapper.readValue(tripsStringInput, AdminTakenTrip[].class);
            for (AdminTakenTrip tripDao : tripsDao) {
                UserDao userDao = tripDao.getPassenger();
                UserDao driverDao = tripDao.getDriver();

                User passenger = new User();
                passenger.setId(userDao.getUserId());
                passenger.setFirstName(userDao.getFirstName());
                passenger.setUserName(userDao.getUserName());
                userService.putUserInfo(userDao.getChatId(), passenger);
                userService.putState(userDao.getChatId(), userDao.getCurrentState());

                User driver = new User();
                driver.setId(driverDao.getUserId());
                driver.setFirstName(driverDao.getFirstName());
                driver.setUserName(driverDao.getUserName());
                userService.putUserInfo(driverDao.getChatId(), driver);
                userService.putState(driverDao.getChatId(), userDao.getCurrentState());

                QueueTrip queueTrip = new QueueTrip();
                queueTrip.setPassengerChatId(userDao.getChatId());
                queueTrip.setAddress(tripDao.getAddress());
                queueTrip.setDetails(tripDao.getDetails());

                tripBuilderService.putPassengerInfo(userDao.getChatId(), queueTrip);
//                tripService.getTakenTripService().addTakenTrip(new TakenTrip(driverDao.getChatId(), tripDao.getAddress(), tripDao.getDetails(), TripStatus.INACTIVE, driverDao.getChatId()));
            }
        }
    }

}
