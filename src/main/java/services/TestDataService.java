package services;

import models.QueueTrip;
import models.utils.TripComparator;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.*;

/**
 * Service that contains test data
 */
public class TestDataService {
    public static Map<Long, User> getTestUserInfo() {
        Map<Long, User> resMap = new HashMap<>();
        resMap.put(1L, new User(1L, "Єгор", false, "не", "kalio", "ua", true, true, true));
//        resMap.put(2L, new User(2L, "Ольга", false, "Геращенко", "kalio", "ua", true, true, true));
//        resMap.put(3L, new User(3L, "Анна", false, "Готенко", "kalio", "ua", true, true, true));
//        resMap.put(4L, new User(4L, "Сєва", false, "Нє Лох", "kalio", "ua", true, true, true));
        return resMap;
    }

    public static Queue<QueueTrip> getTestPassengerQueue() {
        Queue<QueueTrip> resList = new PriorityQueue<>(TripComparator.TRIP_COMPARATOR);
//        resList.add(new QueueTrip(1, "вул. Базарна -231", "Півтори людини і собака"));
//        resList.add(new QueueTrip(2, "вул. Соборна 9999", "Двоє людей, дуже похилого віку.. Upd: Одна людина.."));
//        resList.add(new QueueTrip(3, "вул. Торговельна 3333", "Три кота ж"));
//        resList.add(new QueueTrip(4, "вул. Правобережна 43", "Два коти"));
        return resList;
    }
}
