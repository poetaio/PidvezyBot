package services;

import bots.utils.Constants;
import models.dao.DriverUpdateDao;

import java.util.*;
import java.util.stream.Collectors;

public class DriverUpdateService {
    private static final DriverUpdateService INSTANCE = new DriverUpdateService();
    private PriorityQueue<DriverUpdateDao> driverUpdateQueue;

    private DriverUpdateService() {
        driverUpdateQueue = new PriorityQueue<>();
    }

    public static DriverUpdateService getInstance() {
        return INSTANCE;
    }

    public void resetDriverTime(long chatId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);

        DriverUpdateDao newUpdateDao = new DriverUpdateDao(chatId, calendar.getTime());
        driverUpdateQueue.remove(newUpdateDao);
        driverUpdateQueue.add(newUpdateDao);
    }

    public void addDriver(long chatId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);
        driverUpdateQueue.add(new DriverUpdateDao(chatId, calendar.getTime()));
    }

    public void removeDriver(long chatId) {
        driverUpdateQueue.remove(new DriverUpdateDao(chatId));
    }

    public void removeAll() {
        driverUpdateQueue = new PriorityQueue<>();
    }

    public DriverUpdateDao getDriver(long chatId) {
        Optional<DriverUpdateDao> resDao = driverUpdateQueue.stream().filter(x -> x.getChatId() == chatId).findFirst();
        return resDao.isEmpty() ? null : resDao.get();
    }

    public List<DriverUpdateDao> getAll() {
        return driverUpdateQueue.stream().map(DriverUpdateDao::clone).collect(Collectors.toList());
    }

    public List<Long> getDriversToUpdate() {
        List<Long> resList = new LinkedList<>();
        Date currentDate = new Date();

        for (DriverUpdateDao driverUpdateDao : driverUpdateQueue) {
            if (driverUpdateDao.getNextUpdateTime().after(currentDate)) {
                break;
            }
            resetDriverTime(driverUpdateDao.getChatId());
            resList.add(driverUpdateDao.getChatId());
        }

        return resList;
    }

    @Override
    public String toString() {
        return driverUpdateQueue.toString();
    }
}
