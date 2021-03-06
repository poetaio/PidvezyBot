package services;

import models.utils.State;

import org.telegram.telegrambots.meta.api.objects.User;
import repositories.UserRepository;
import services.driver_services.DriverService;
import services.trip_services.TripService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UserService {
//     singleton
    private static UserService INSTANCE;

    public static void initializeInstance(Map<Long, State> userStatesMap, Map<Long, User> userInfo, Map<Long, String> userNumbers) {
        if (INSTANCE != null)
            throw new RuntimeException("Instance already initialized");
        INSTANCE = new UserService(userStatesMap, userInfo, userNumbers);
    }

    public static UserService getInstance() {
        if (INSTANCE == null)
            throw new RuntimeException("Instance has not been initialized");
        return INSTANCE;
    }

    private final DriverService driverService;
    private final TripService tripService;
    private final UserRepository userRepository;

    private final Map<Long, User> userInfoMap;
    private final Map<Long, State> userStateMap;
    private final Map<Long, String> usersNumbersMap;

    private UserService(Map<Long, State> userStateMap, Map<Long, User> userInfoMap,
                       Map<Long, String> usersNumberMap) {
        this.userInfoMap = userInfoMap;
        this.usersNumbersMap = usersNumberMap;

        userRepository = new UserRepository();
        this.userStateMap = userStateMap;

        this.driverService = DriverService.getInstance();
        this.tripService = TripService.getInstance();
    }

    /**
     * Get user entity (first name, last name, username...) by user chat id with bot
     * @param chatId user id of the chat with bot
     * @return user entity
     */
    public User getUserInfo(long chatId) {
        return userInfoMap.get(chatId);
    }

    /**
     * Set user entity (first name, last name, username...) to match user chat id with bot
     * @param chatId user chat id
     * @param user user entity
     */
    public void putUserInfo(long chatId, User user) {
        userInfoMap.put(chatId, user);
        userRepository.setUserInfo(chatId, user);
    }

    /**
     * Cleans up all user data
     * @param chatId id of the chat with bot
     */
    public void performCleanup(long chatId) {
        tripService.removeTripFromQueueByPassengerId(chatId);
        tripService.cancelTripOnSearchStopped(chatId);
        driverService.removeDriver(chatId);
        // not deleting user, as info+trips stay in db
    }

    /**
     * Get current state of the user chat by chat id
     * @param chatId id of the chat with bot
     * @return returns current state of the user chat
     */
    public State getState(long chatId) {
        return userStateMap.get(chatId);
    }

    /**
     * Set current state of the user chat
     * @param chatId id of the chat with bot
     * @param state current state of the chat
     */
    public void putState(long chatId, State state) {
        userStateMap.put(chatId, state);
        userRepository.setUserState(chatId, state);
    }

    public void addNumber(long chatId, String number) {
        String encryptedNumber = EncryptionService.encrypt(number);
        usersNumbersMap.put(chatId, encryptedNumber);
        CompletableFuture.runAsync(() -> userRepository.setNumber(chatId, encryptedNumber));
    }

    public String getNumber(long chatId) {
        return EncryptionService.decrypt(usersNumbersMap.get(chatId));
    }

}
