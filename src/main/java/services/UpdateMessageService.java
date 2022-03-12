package services;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that saves last messages sent by bot and user to update them instead of sending new ones
 */
public class UpdateMessageService {
    private static final UpdateMessageService INSTANCE =
            new UpdateMessageService();

    private final Map<Long, Integer> botMessageToUpdateId;
    private final Map<Long, Integer> userMessageToUpdateId;

    private UpdateMessageService() {
        botMessageToUpdateId = new HashMap<>();
        userMessageToUpdateId = new HashMap<>();
    }

    public static UpdateMessageService getInstance() {
        return INSTANCE;
    }

    public void putBotMessageToUpdate(long userChatId, int botMessageToUpdateId) {
        this.botMessageToUpdateId.put(userChatId, botMessageToUpdateId);
    }

    public Integer getBotMessageToUpdate(long userChatId) {
        return botMessageToUpdateId.get(userChatId);
    }

    public void putUserMessageToUpdate(long userChatId, int userMessageToUpdateId) {
        this.userMessageToUpdateId.put(userChatId, userMessageToUpdateId);
    }

    public Integer getUserMessageToUpdate(long userChatId) {
        return userMessageToUpdateId.get(userChatId);
    }
}
