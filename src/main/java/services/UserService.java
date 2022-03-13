package services;

import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    private static final UserService INSTANCE = new UserService();
    private final Map<Long, User> userInfo;

    public static UserService getInstance() {
        return INSTANCE;
    }

    private UserService() {
        userInfo = TestDataService.getTestUserInfo();
    }

    public User getUserInfo(long chatId) {
        return userInfo.get(chatId);
    }

    public void putUserInfo(long chatId, User user) {
        // TODO: make copy
        userInfo.put(chatId, user);
    }

    public void performCleanup(long chatId) {
        PassengerService.getInstance().removeTripInfo(chatId);
        DriverService.getInstance().removeDriver(chatId);
    }

}
