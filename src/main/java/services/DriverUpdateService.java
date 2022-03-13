package services;

import bots.utils.Constants;
import models.dao.DriverUpdateDao;

import java.util.*;
import java.util.stream.Collectors;

public class DriverUpdateService {
    private static final DriverUpdateService INSTANCE = new DriverUpdateService();
    private final PriorityQueue<DriverUpdateDao> driverUpdateQueue;

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
        driverUpdateQueue.clear();
    }

    public DriverUpdateDao getDriver(long chatId) {
        Optional<DriverUpdateDao> resDao = driverUpdateQueue.stream().filter(x -> x.getChatId() == chatId).findFirst();
        return resDao.isEmpty() ? null : resDao.get();
    }

    public List<DriverUpdateDao> getAll() {
        return driverUpdateQueue.stream().map(DriverUpdateDao::clone).collect(Collectors.toList());
    }

    /**
     * Get drivers with date of update that has already passed
     * @return List of drivers' id's
     */
    public List<Long> getDriversToUpdate() {
        List<DriverUpdateDao> resList = new LinkedList<>();
        Date currentDate = new Date();

        // find all driver that require update
        for (DriverUpdateDao driverUpdateDao : driverUpdateQueue) {
            if (driverUpdateDao.getNextUpdateTime().after(currentDate)) {
                break;
            }

            resList.add(driverUpdateDao);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);

        // reinsert found drivers with new date value
        for (DriverUpdateDao driverUpdateDao : resList) {
            driverUpdateDao.setNextUpdateTime(calendar.getTime());
            driverUpdateQueue.remove(driverUpdateDao);
            driverUpdateQueue.add(driverUpdateDao);
        }

        return resList.stream().map(DriverUpdateDao::getChatId).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return driverUpdateQueue.toString();
    }
}
