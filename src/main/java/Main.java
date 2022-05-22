import bots.PidvesyBot;
import bots.ResponseHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import server.AdminServer;
import services.admin_services.AdminService;

import java.io.IOException;
import java.util.TimeZone;

public class Main {
    public static void main(String[] args) {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"));
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new PidvesyBot());
            ResponseHandler responseHandler = ResponseHandler.getInstance(null);
            AdminService adminService = responseHandler.createAdminService();
            new Thread(() -> {
                try {
                    AdminServer adminServer = new AdminServer(adminService);
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
