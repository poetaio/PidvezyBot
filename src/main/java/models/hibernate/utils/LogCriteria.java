package models.hibernate.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.utils.State;

import java.sql.Timestamp;

@Getter
@AllArgsConstructor
public class LogCriteria {
    private Timestamp dateFrom;
    private Timestamp dateTo;
    private State stateFrom;
    private State stateTo;
    private Long userId;
}
