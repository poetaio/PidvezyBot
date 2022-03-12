package services;

import java.util.HashMap;
import java.util.Map;

public class DriverUpdateMessageService {
    private static final DriverUpdateMessageService INSTANCE =
            new DriverUpdateMessageService();

    private final Map<Long, Integer> driverMessageToUpdateId;

    private DriverUpdateMessageService() {
        driverMessageToUpdateId = new HashMap<>();
    }

    public static DriverUpdateMessageService getInstance() {
        return INSTANCE;
    }

    public void putMessageToUpdate(long driverChatId, int messageToUpdateId) {
        driverMessageToUpdateId.put(driverChatId, messageToUpdateId);
    }

    public Integer getMessageToUpdate(long driverChatId) {
        return driverMessageToUpdateId.get(driverChatId);
    }
}
