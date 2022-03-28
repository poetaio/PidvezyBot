package models.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.hibernate.utils.GroupStatus;
import models.hibernate.utils.GroupType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDao {
    private Long groupId;
    private String groupName;
    private GroupType groupType;

    public GroupDao(long groupId, GroupType groupType) {
        this.groupId = groupId;
        this.groupType = groupType;
    }
}
