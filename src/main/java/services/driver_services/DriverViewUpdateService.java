package services.driver_services;

import bot.utils.Constants;
import models.dao.DriverUpdateDao;
import services.event_service.EventService;
import services.event_service.utils.Events;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that contains drivers and their date/time of the next view (passenger trip application) update
 */
public class DriverViewUpdateService {
    // no need of pq here :( all drivers are set +15 seconds,
    // and automatically moved to the end of the queue
    // cause others have then <=15 left
    // aaand they're automatically sorted.. by the time of the insertion
//    private final PriorityQueue<DriverUpdateDao> driverUpdateQueue;
    private final List<DriverUpdateDao> driverUpdateQueue;

    public DriverViewUpdateService(List<DriverUpdateDao> driverUpdateDaos) {
        driverUpdateQueue = driverUpdateDaos;
    }

    /**
     * Subscribe driver for view update
     * @param driverChatId driver chat id
     */
    public void addDriver(long driverChatId) {
        if (driverUpdateQueue.isEmpty())
            EventService.getInstance().notify(Events.DRIVER_QUEUE_NON_EMPTY_EVENT, null);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);
        driverUpdateQueue.add(new DriverUpdateDao(driverChatId, calendar.getTime()));
    }

    public void removeDriver(long driverChatId) {
        if (driverUpdateQueue.remove(new DriverUpdateDao(driverChatId)) && driverUpdateQueue.isEmpty()) {
            EventService.getInstance().notify(Events.DRIVER_QUEUE_EMPTY_EVENT, null);
        }
    }

    public void removeAll() {
        driverUpdateQueue.clear();
    }

    public DriverUpdateDao getDriver(long driverChatId) {
        Optional<DriverUpdateDao> resDao = driverUpdateQueue.stream().filter(x -> x.getChatId() == driverChatId).findFirst();
        return resDao.isEmpty() ? null : resDao.get();
    }

    public List<DriverUpdateDao> getAll() {
        return driverUpdateQueue.stream().map(DriverUpdateDao::clone).collect(Collectors.toList());
    }

    public List<Long> getAllActiveDriversIds() {
        return driverUpdateQueue.stream().map(DriverUpdateDao::getChatId).collect(Collectors.toList());
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

    /**
     * Reset driver's date/time of the next view update (set to now + Constants.DRIVER_UPDATE_INTERVAL)
     * @param driverChatId driver chat id
     */
    public void resetDriverTime(long driverChatId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);

        DriverUpdateDao newUpdateDao = new DriverUpdateDao(driverChatId, calendar.getTime());
        driverUpdateQueue.remove(newUpdateDao);
        driverUpdateQueue.add(newUpdateDao);
    }

    public List<DriverUpdateDao> getDriverUpdateQueueList(){
        return driverUpdateQueue;
    }

    @Override
    public String toString() {
        return driverUpdateQueue.toString();
    }
}
