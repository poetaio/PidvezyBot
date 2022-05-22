package services;

import models.dao.LogDao;
import repositories.LogRepository;

public class LogService {
    private final LogRepository logRepository;

    public LogService() {
        this.logRepository = new LogRepository();
    }

    public void createLog(LogDao logDao) {
        logRepository.createLog(logDao);
    }
}
