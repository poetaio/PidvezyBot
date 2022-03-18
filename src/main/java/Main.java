import bots.PidvesyBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import server.AdminServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new PidvesyBot());
            new Thread(() -> {
                try {
                    AdminServer adminServer = new AdminServer();
                    adminServer.startAdminSever();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
