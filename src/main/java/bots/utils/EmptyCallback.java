package bots.utils;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;
import services.LogService;

/**
 * Do nothing callback
 */
public class EmptyCallback implements SentCallback<Message> {
    private static final LogService logService = new LogService();
    @Override
    public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
//        logService.logMessage(message.getChatId(), message.getMessageId(), message.getText());
    }

    @Override
    public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {}

    @Override
    public void onException(BotApiMethod<Message> botApiMethod, Exception e) {}
}
