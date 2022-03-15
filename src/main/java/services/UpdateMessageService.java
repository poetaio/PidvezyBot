package services;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that saves last messages sent by bot and user
 * to update them instead of sending new ones
 */
public class UpdateMessageService {
    // singleton pattern
    private static final UpdateMessageService INSTANCE = new UpdateMessageService();
    public static UpdateMessageService getInstance() {
        return INSTANCE;
    }

    private final Map<Long, Integer> botMessageToUpdateId;
    private final Map<Long, Integer> userMessageToUpdateId;

    private UpdateMessageService() {
        botMessageToUpdateId = new HashMap<>();
        userMessageToUpdateId = new HashMap<>();
    }

    /**
     * Update last bot message sent to user
     * @param userChatId user chat id
     * @param botMessageToUpdateId id of the message sent by bot
     */
    public void putBotMessageToUpdate(long userChatId, int botMessageToUpdateId) {
        this.botMessageToUpdateId.put(userChatId, botMessageToUpdateId);
    }

    /**
     * Get last bot message sent to user
     * @param userChatId user chat id
     * @return last bot message in chat with user
     */
    public Integer getBotMessageToUpdate(long userChatId) {
        return botMessageToUpdateId.get(userChatId);
    }

    /**
     * Update last user message sent to bot
     * @param userChatId user chat id
     * @param userMessageToUpdateId message sent by user
     */
    public void putUserMessageToUpdate(long userChatId, int userMessageToUpdateId) {
        this.userMessageToUpdateId.put(userChatId, userMessageToUpdateId);
    }

    /**
     * Get last user message sent to bot
     * @param userChatId user chat id
     * @return last user message sent to bot
     */
    public Integer getUserMessageToUpdate(long userChatId) {
        return userMessageToUpdateId.get(userChatId);
    }
}
