package repositories;

import models.hibernate.Group;
import models.hibernate.GroupMessage;
import models.hibernate.Trip;
import models.hibernate.utils.GroupCriteria;
import models.hibernate.utils.GroupStatus;
import models.hibernate.utils.GroupType;
import org.hibernate.Session;
import org.hibernate.query.Query;
import repositories.utils.CountGroupDao;
import server.utils.Constants;
import utils.HibernateUtil;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class GroupRepository {
    private final TripRepository tripRepository;
    private final GroupMessageRepository groupMessageRepository;

    public GroupRepository() {
        tripRepository = new TripRepository();
        groupMessageRepository = new GroupMessageRepository();
    }

    public Group getGroupWithDefault(Session session, long groupId) {
        Group group = session.get(Group.class, groupId);
        if (group == null) {
            group = new Group(groupId);
            group.setGroupType(GroupType.GROUP);
            session.persist(group);
        }
        return group;
    }

    public Group getChannelWithDefault(Session session, long groupId) {
        Group group = session.get(Group.class, groupId);
        if (group == null) {
            group = new Group(groupId);
            group.setGroupType(GroupType.CHANNEL);
            session.persist(group);
        }
        return group;
    }

    public void removeGroup(long chatId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        session.delete(getGroupWithDefault(session, chatId));

        session.getTransaction().commit();
    }

    public void setGroupActive(long groupId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Group group = getGroupWithDefault(session, groupId);
        group.setGroupStatus(GroupStatus.ACTIVE);

        session.getTransaction().commit();
    }

    public void setGroupInactive(long groupId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Group group = getGroupWithDefault(session, groupId);
        group.setGroupStatus(GroupStatus.INACTIVE);

        session.getTransaction().commit();
    }

    public void setGroupName(long groupId, String groupName) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Group group = getGroupWithDefault(session, groupId);
        group.setGroupName(groupName);

        session.getTransaction().commit();
    }

    public void setChannelName(long channelId, String channelName) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Group channel = getChannelWithDefault(session, channelId);
        channel.setGroupName(channelName);

        session.getTransaction().commit();
    }

    public CountGroupDao getAll(Integer page, Integer limit, GroupCriteria groupCriteria) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);

        Predicate predicate = cb.conjunction();

        if (groupCriteria.getGroupId() != null) {
            predicate = cb.and(predicate, cb.like(root.get("groupId"), "%"+groupCriteria.getGroupId()+"%"));
        }
        if (groupCriteria.getGroupName() != null) {
            predicate = cb.and(predicate, cb.like(root.get("groupName"), "%"+groupCriteria.getGroupName()+"%"));
        }
        if (groupCriteria.getGroupStatus() != null) {
            predicate = cb.and(predicate, cb.equal(root.get("groupStatus"), groupCriteria.getGroupStatus()));
        }

        cq.select(root).where(predicate);

        limit = limit == null || limit < 1 ? Constants.DEFAULT_LIMIT : limit;
        int offset;
        if (page != null) offset = (page - 1) * limit;
        else offset = 0;

        Query<Group> query = session.createQuery(cq)
                .setFirstResult(offset)
                .setMaxResults(limit);

        List<Group> resGroups = query.getResultList();

        // counting results
        CriteriaQuery<Long> cqCount = cb.createQuery(Long.class);
        cqCount.select(cb.count(cqCount.from(Group.class))).where(predicate);
        Long totalNumber = session.createQuery(cqCount).getSingleResult();

        session.getTransaction().commit();

        return new CountGroupDao(totalNumber, resGroups);
    }

    public void setMessageIdByGroupAndTrip(long groupId, UUID tripId, Integer messageId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Group group = getGroupWithDefault(session, groupId);
        if (group.getGroupMessages() == null)
            group.setGroupMessages(new HashSet<>());

        Trip trip = tripRepository.getTripWithDefault(session, tripId);
        GroupMessage groupMessage = groupMessageRepository.getGroupMessageWithDefault(session, group, trip);
        groupMessage.setMessageId(messageId);

        session.getTransaction().commit();
    }

    public void removeMessageIdByGroupAndTrip(long groupId, UUID tripId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Group group = getGroupWithDefault(session, groupId);
        if (group.getGroupMessages() == null)
            return;

        Trip trip = tripRepository.getTripWithDefault(session, tripId);
        GroupMessage groupMessage = groupMessageRepository.getGroupMessageWithDefault(session, group, trip);
        session.delete(groupMessage);

        session.getTransaction().commit();
    }
}
