package bot.utils;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

public class EmptyDeleteCallback implements SentCallback<Boolean> {
    @Override
    public void onResult(BotApiMethod<Boolean> botApiMethod, Boolean aBoolean) {}

    @Override
    public void onError(BotApiMethod<Boolean> botApiMethod, TelegramApiRequestException e) {}

    @Override
    public void onException(BotApiMethod<Boolean> botApiMethod, Exception e) {}
}
