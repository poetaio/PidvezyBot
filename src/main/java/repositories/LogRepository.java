package repositories;

import models.dao.LogDao;
import models.hibernate.Log;
import org.hibernate.Session;
import models.hibernate.User;
import utils.HibernateUtil;

public class LogRepository {
    private UserRepository userRepository;

    public LogRepository() {
        userRepository = new UserRepository();
    }

    public void createLog(LogDao logDao) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        User user = userRepository.getWithDefault(session, logDao.getUserId());
        session.persist(new Log(user, logDao));

        session.getTransaction().commit();
    }
}
