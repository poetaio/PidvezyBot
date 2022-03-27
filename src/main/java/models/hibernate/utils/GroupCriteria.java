package models.hibernate.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.utils.GroupStatus;

@Getter
@AllArgsConstructor
public class GroupCriteria {
    private Long groupId;
    private String groupName;
    private GroupStatus groupStatus;
}
