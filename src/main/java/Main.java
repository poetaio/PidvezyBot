import bots.PidvesyBot;
import bots.ResponseHandler;
import models.hibernate.Trip;
import models.hibernate.User;
import org.hibernate.Session;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import server.AdminServer;
import services.admin_services.AdminService;
import utils.HibernateUtil;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
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
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

//        Session session = HibernateUtil.getSessionFactory().openSession();
//        session.beginTransaction();
//
//        User user = new User(12341);
//        user.setLastName("lastNaeklajfasdiljfasdiofjkasndklfjaslkf");
//        session.saveOrUpdate(user);
//
//        Trip trip = new Trip(user);
//        session.saveOrUpdate(trip);
//
//        trip.setTakenByDriver(user);
//        session.saveOrUpdate(trip);
//
//        System.out.println(trip.getPassenger().getFirstName());
//        System.out.println(user.getFirstName());
////        List<Driver> driverList = session.createQuery
////                ("SELECT D FROM Driver D", Driver.class).getResultList();
//
//        session.getTransaction().commit();
//        HibernateUtil.shutdown();
    }
}
