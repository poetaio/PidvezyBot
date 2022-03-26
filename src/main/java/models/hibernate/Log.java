package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.dao.LogDao;
import models.hibernate.utils.MapToJsonConverter;
import models.utils.State;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Map;

@AllArgsConstructor
@Setter
@Getter
@Entity(name = "logs")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "log_id")
    private long logId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_from")
    private State stateFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_to")
    private State stateTo;

    @Basic
    private Timestamp logTime;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "log_info")
    private Map<String, Object> logInfo;

    public Log() {
        logTime = new Timestamp(System.currentTimeMillis());
    }

    public Log(User user, LogDao logDao) {
        this();
        this.user = user;
        stateFrom = logDao.getStateFrom();
        stateTo = logDao.getStateTo();
        logInfo = logDao.getLogInfo();
        message = logDao.getMessage();
    }
}
