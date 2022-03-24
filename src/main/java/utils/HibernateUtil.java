package utils;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();
//    private static final Session session = sessionFactory.openSession();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration cfg = new Configuration().configure("/hibernate.cfg.xml");
            cfg.setProperty("hibernate.connection.url", System.getenv("DATABASE_URLL"));
            cfg.setProperty("hibernate.connection.username", System.getenv("DATABASE_USER"));
            cfg.setProperty("hibernate.connection.password", System.getenv("DATABASE_PASSWORD"));
            cfg.setProperty("hibernate.hbm2ddl.auto", System.getenv("DATABASE_UPDATE"));
            return cfg.buildSessionFactory();
        } catch (HibernateException he) {
            System.out.println("Session Factory creation failure");
            throw he;
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static Session getSession() {
        return sessionFactory.openSession();
    }

    public static void shutdown() {
        getSessionFactory().close();
    }

}
