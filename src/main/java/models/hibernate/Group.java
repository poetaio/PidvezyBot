package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.utils.GroupStatus;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "groups")
public class Group {
    @Id
    @Column(name = "group_id")
    private long groupId;

    @Column(name = "group_name")
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status")
    private GroupStatus groupStatus;

    @OneToMany(mappedBy = "group")
    private Set<GroupMessage> groupMessages = new HashSet<>();

    public Group(long groupId) {
        this.groupId = groupId;
        groupStatus = GroupStatus.ACTIVE;
    }
}
