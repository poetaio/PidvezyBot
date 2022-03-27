import bots.pidvezy_chat_bot.PidvesyBot;
import bots.pidvezy_chat_bot.ResponseHandler;
import bots.pidvezy_group_bot.PidvezyGroupBot;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import server.AdminServer;
import services.PersistenceService;
import services.admin_services.AdminService;

import java.io.IOException;
import java.util.TimeZone;

public class Main {
    public static void main(String[] args) {
        try {
            PersistenceService.initServices();

            TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"));
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);

            api.registerBot(new PidvesyBot());
            api.registerBot(new PidvezyGroupBot());
            new Thread(() -> {
                try {
                    AdminServer adminServer = new AdminServer();
                    adminServer.startAdminSever();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (TelegramApiException | JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
