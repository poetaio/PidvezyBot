package repositories;

import models.dao.adminDao.LogDao;
import models.hibernate.Log;
import models.hibernate.utils.LogCriteria;
import org.hibernate.Session;
import models.hibernate.User;
import org.hibernate.query.Query;
import server.utils.Constants;
import repositories.utils.CountLogDao;
import utils.HibernateUtil;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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

    public CountLogDao getAll(Integer page, Integer limit, LogCriteria logCriteria) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        // results
        CriteriaQuery<Log> cq = cb.createQuery(Log.class);
        Root<Log> root = cq.from(Log.class);
        cq.select(root);

        Predicate finalPredicate = cb.conjunction();

        if (logCriteria.getUserId() != null) {
            finalPredicate = cb.and(finalPredicate, cb.equal(root.get("user"), logCriteria.getUserId()));
        }
        if (logCriteria.getStateFrom() != null) {
            finalPredicate = cb.and(finalPredicate, cb.equal(root.get("stateFrom"), logCriteria.getStateFrom()));
        }
        if (logCriteria.getStateTo() != null) {
            finalPredicate = cb.and(finalPredicate, cb.equal(root.get("stateTo"), logCriteria.getStateTo()));
        }
        if (logCriteria.getDateFrom() != null) {
            finalPredicate = cb.and(finalPredicate, cb.greaterThanOrEqualTo(root.get("logTime"), logCriteria.getDateFrom()));
        }
        if (logCriteria.getDateTo() != null) {
            finalPredicate = cb.and(finalPredicate, cb.lessThanOrEqualTo(root.get("logTime"), logCriteria.getDateTo()));
        }

        cq.where(finalPredicate);

        limit = limit == null || limit < 1 ? Constants.DEFAULT_LIMIT : limit;

        int offset;
        if (page != null) offset = (page - 1) * limit;
        else offset = 0;

        Query<Log> query = session.createQuery(cq)
                .setFirstResult(offset)
                .setMaxResults(limit);

        // results total number
        CriteriaQuery<Long> cqCount = cb.createQuery(Long.class);
        cqCount.select(cb.count(cqCount.from(Log.class))).where(finalPredicate);
        long totalResultsNumber = session.createQuery(cqCount).getSingleResult();

        session.getTransaction().commit();

        return new CountLogDao(totalResultsNumber, query.getResultList());
    }
}
