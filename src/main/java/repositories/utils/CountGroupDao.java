package repositories.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.hibernate.Group;

import java.util.List;

@Getter
@AllArgsConstructor
public class CountGroupDao {
    private Long totalNumber;
    private List<Group> groups;
}
