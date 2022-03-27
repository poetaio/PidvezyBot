package models.dao.adminDao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import models.utils.State;

import java.util.HashMap;
import java.util.Map;

@Getter
//@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogDao {
    private long userId;
    private State stateFrom;
    private State stateTo;
    private Map<String, Object> logInfo;
    private String message;
//    private Timestamp logTime;

    public static LogDaoBuilder builder() {
        return new LogDaoBuilder();
    }

    public static class LogDaoBuilder {
        private long userId;
        private State stateFrom;
        private State stateTo;
        private Map<String, Object> logInfo;
        private String message;
//        private Timestamp logTime;

        private LogDaoBuilder() {}

        public void userId(long userId) {
            this.userId = userId;
        }

        public void stateFrom(State stateFrom) {
            this.stateFrom = stateFrom;
        }

        public void stateTo(State stateTo) {
            this.stateTo = stateTo;
        }

        public void logInfo(Map<String, Object> logInfo) {
            this.logInfo = logInfo;
        }

        public void putLogInfo(String s, Object o) {
            if (logInfo == null)
                logInfo = new HashMap<>();
            logInfo.put(s, o);
        }

        public void message(String message) {
            this.message = message;
        }

//        public void logTime(Timestamp logTime) {
//            this.logTime = logTime;
//        }

        public LogDao build() {
            return new LogDao(userId, stateFrom, stateTo, logInfo, message);
        }
    }
}
