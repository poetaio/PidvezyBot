package bots;

import bots.response_handler.ResponseHandler;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BroadcastBot {
    public static void main(String[] args) throws TelegramApiException {
        ResponseHandler.getInstance(null).sendBroadcastToDrivers("Це повідомлення для водіїв!");
    }
}
