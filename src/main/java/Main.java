import bots.PidvesyBot;
import models.Driver;
import org.hibernate.Session;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import utils.HibernateUtil;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new PidvesyBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

//        Session session = HibernateUtil.getSessionFactory().openSession();
//        session.beginTransaction();
//
//        Driver driver = new Driver(1234);
//        session.save(driver);
//
//        List<Driver> driverList = session.createQuery
//                ("SELECT D FROM Driver D", Driver.class).getResultList();
//        System.out.println("Drivers: " + driverList);
//
//        session.getTransaction().commit();
//        HibernateUtil.shutdown();
    }
}
