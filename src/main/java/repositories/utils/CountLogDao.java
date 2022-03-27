package repositories.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.hibernate.Log;

import java.util.List;

@Getter
@AllArgsConstructor
public class CountLogDao {
    private long total;
    private List<Log> resList;
}
