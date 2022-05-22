package bots.utils;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

/**
 * Do nothing callback
 */
public interface ResultCallback extends SentCallback<Message> {
    void onResult(BotApiMethod<Message> botApiMethod, Message message);

    @Override
    default void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {}

    @Override
    default void onException(BotApiMethod<Message> botApiMethod, Exception e) {}
}
