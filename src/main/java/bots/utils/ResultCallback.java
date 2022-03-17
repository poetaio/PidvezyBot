package bots.utils;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

/**
 * Do nothing callback
 */
public abstract class ResultCallback implements SentCallback<Message> {
    public abstract void onResult(BotApiMethod<Message> botApiMethod, Message message);

    @Override
    public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {}

    @Override
    public void onException(BotApiMethod<Message> botApiMethod, Exception e) {}
}
