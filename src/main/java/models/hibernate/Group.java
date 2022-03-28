package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.hibernate.utils.GroupStatus;
import models.hibernate.utils.GroupType;

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
    @Column(name = "group_type")
    private GroupType groupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status")
    private GroupStatus groupStatus;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private Set<GroupMessage> groupMessages = new HashSet<>();

    public Group(long groupId) {
        this.groupId = groupId;
        groupStatus = GroupStatus.ACTIVE;
    }
}
