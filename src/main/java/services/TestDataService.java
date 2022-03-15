package services;

import models.Trip;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that contains test data
 */
public class TestDataService {
    public static Map<Long, User> getTestUserInfo() {
        Map<Long, User> resMap = new HashMap<>();
//        resMap.put(1L, new User(1L, "Єгор", false, "не", "kalio", "ua", true, true, true));
//        resMap.put(2L, new User(2L, "Ольга", false, "Геращенко", "kalio", "ua", true, true, true));
//        resMap.put(3L, new User(3L, "Анна", false, "Готенко", "kalio", "ua", true, true, true));
//        resMap.put(4L, new User(4L, "Сєва", false, "Лох", "kalio", "ua", true, true, true));

        return resMap;
    }

    public static List<Trip> getTestPassengerQueue() {
        List<Trip> resList = new ArrayList<>();
//        resList.add(new QueuePassengerDao(1, "вул. Базарна -231", "Півтори людини і собака"));
//        resList.add(new QueuePassengerDao(2, "вул. Соборна 9999", "Двоє людей, дуже похилого віку.. Upd: Одна людина.."));
//        resList.add(new QueuePassengerDao(3, "вул. Торговельна 3333", "Три кота ж"));
//        resList.add(new QueuePassengerDao(4, "вул. Правобережна 43", "Два коти"));

        return resList;
    }
}
