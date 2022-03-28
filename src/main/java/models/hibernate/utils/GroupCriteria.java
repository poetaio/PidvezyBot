package models.hibernate.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupCriteria {
    private Long groupId;
    private String groupName;
    private GroupStatus groupStatus;
}
