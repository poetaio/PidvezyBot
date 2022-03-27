package bots.pidvezy_group_bot.utils;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.io.Serializable;

public class EmptyEditCallback implements SentCallback<Serializable> {
    @Override
    public void onResult(BotApiMethod<Serializable> botApiMethod, Serializable serializable) {}

    @Override
    public void onError(BotApiMethod<Serializable> botApiMethod, TelegramApiRequestException e) {}

    @Override
    public void onException(BotApiMethod<Serializable> botApiMethod, Exception e) {}
}
