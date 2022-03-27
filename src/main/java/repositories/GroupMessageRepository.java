package repositories;

import models.hibernate.Group;
import models.hibernate.GroupMessage;
import models.hibernate.GroupMessageId;
import models.hibernate.Trip;
import org.hibernate.Session;

public class GroupMessageRepository {
    public GroupMessage getGroupMessageWithDefault(Session session, Group group, Trip trip) {
        GroupMessage groupMessage = session.get(GroupMessage.class, new GroupMessageId(group.getGroupId(), trip.getTripId()));
        if (groupMessage == null) {
            groupMessage = new GroupMessage();
            groupMessage.setGroup(group);
            groupMessage.setTrip(trip);
            session.persist(groupMessage);
        }
        return groupMessage;
    }
}
