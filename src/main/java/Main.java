import bot.PidvesyBot;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import server.AdminServer;
import services.PersistenceService;

import java.io.IOException;
import java.util.TimeZone;

public class Main {
    public static void main(String[] args) {
        try {
            PersistenceService.initServices();

            TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"));
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new PidvesyBot());
            new Thread(() -> {
                try {
                    AdminServer adminServer = new AdminServer();
                    adminServer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (TelegramApiException | JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
