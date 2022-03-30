package services;

import models.dao.adminDao.LogDao;
import models.hibernate.utils.LogCriteria;
import repositories.LogRepository;
import repositories.utils.CountLogDao;

public class LogService {
    private final LogRepository logRepository;

    public LogService() {
        this.logRepository = new LogRepository();
    }

    public void createLog(LogDao logDao) {
        logRepository.createLog(logDao);
    }
    public CountLogDao getAll(Integer page, Integer limit, LogCriteria logCriteria) {
        return logRepository.getAll(page, limit, logCriteria);
    }

    public void logMessage(long chatId, int messageId, String messageText) {
        logRepository.logMessage(chatId, HashService.hashMessage(messageText));
    }
}
