package repositories;

import models.utils.State;
import org.hibernate.Session;
import models.hibernate.User;
import utils.HibernateUtil;

public class UserRepository {
    /**
     * Get existing entity in persist state, or create new one and make state persist
     * @param session current session
     * @param userId user id
     * @return returns existing or new persistent user
     */
    public User getWithDefault(Session session, long userId) {
        User user = session.get(User.class, userId);
        if (user == null) {
            user = new User(userId);
            // no need to generate id (that's why no save)
            session.persist(user);
        }
        return user;
    }

    public void setUserInfo(long userId, org.telegram.telegrambots.meta.api.objects.User user) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        User dbUser = getWithDefault(session, userId);
        dbUser.setUsername(user.getUserName());
        dbUser.setFirstName(user.getFirstName());
        dbUser.setLastName(user.getLastName());

        session.getTransaction().commit();
    }

    public void setNumber(long userId, String number) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        getWithDefault(session, userId).setPhoneNumber(number);

        session.getTransaction().commit();
    }

    public void setUserState(long userId, State userState) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        getWithDefault(session, userId).setUserState(userState);

        session.getTransaction().commit();
    }
}
