package utils;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration cfg = new Configuration().configure("/hibernate.cfg.xml");
//            cfg.setProperty("hibernate.connection.url", System.getenv("DATABASE_URL"));
//            cfg.setProperty("hibernate.connection.username", System.getenv("DATABASE_USER"));
//            cfg.setProperty("hibernate.connection.password", System.getenv("DATABASE_PASSWORD"));
//            cfg.setProperty("hibernate.hbm2ddl.auto", System.getenv("DATABASE_UPDATE"));
            cfg.setProperty("hibernate.connection.url", "jdbc:postgresql://ec2-52-44-209-165.compute-1.amazonaws.com:5432/d3u8vn2n1b906a");
            cfg.setProperty("hibernate.connection.username", "mcptayujkwzsgx");
            cfg.setProperty("hibernate.connection.password", "e8da5ddd58099fc0ac6955364063f887b893681b9488b98488f62864633a9bf9");
            cfg.setProperty("hibernate.hbm2ddl.auto", "update");
            return cfg.buildSessionFactory();
        } catch (HibernateException he) {
            System.out.println("Session Factory creation failure");
            throw he;
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }

}
