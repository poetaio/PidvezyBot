package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Setter
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "group_message")
public class GroupMessage {
    @EmbeddedId
    private GroupMessageId groupMessageId = new GroupMessageId();

//    @ManyToOne(cascade = CascadeType.ALL)
    @ManyToOne
//    @OnDelete(action = OnDeleteAction.CASCADE)
    @MapsId("groupId")
    private Group group;

    @ManyToOne
    @MapsId("tripId")
    private Trip trip;

    private Integer messageId;

    public GroupMessage(Group group, Trip trip, Integer messageId) {
        this.group = group;
        this.trip = trip;
        this.messageId = messageId;
    }
}