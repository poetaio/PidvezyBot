package services;

import models.utils.State;

import org.telegram.telegrambots.meta.api.objects.User;
import services.driver_services.DriverService;
import services.passenger_services.PassengerService;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    // singleton pattern
    private static final UserService INSTANCE = new UserService();
    public static UserService getInstance() {
        return INSTANCE;
    }

    // chat id - user
    private final Map<Long, User> userInfo;
    // chat id - state
    private final Map<Long, State> userState;

    private UserService() {
        userInfo = TestDataService.getTestUserInfo();
        userState = new HashMap<>();
    }

    /**
     * Get user entity (first name, last name, username...) by user chat id with bot
     * @param chatId user id of the chat with bot
     * @return user entity
     */
    public User getUserInfo(long chatId) {
        return userInfo.get(chatId);
    }

    /**
     * Set user entity (first name, last name, username...) to match user chat id with bot
     * @param chatId user chat id
     * @param user user entity
     */
    public void putUserInfo(long chatId, User user) {
        // TODO: make copy
        userInfo.put(chatId, user);
    }

    /**
     * Cleans up all user data
     * @param chatId id of the chat with bot
     */
    public void performCleanup(long chatId) {
        PassengerService.getInstance().removeTripInfo(chatId);
        DriverService.getInstance().removeDriver(chatId);
    }

    /**
     * Get current state of the user chat by chat id
     * @param chatId id of the chat with bot
     * @return returns current state of the user chat
     */
    public State getState(long chatId) {
        return userState.get(chatId);
    }

    /**
     * Set current state of the user chat
     * @param chatId id of the chat with bot
     * @param state current state of the chat
     */
    public void putState(long chatId, State state) {
        userState.put(chatId, state);
    }
}
