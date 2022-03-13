package services;

import models.dao.QueuePassengerDao;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDataService {
    public static Map<Long, User> getTestUserInfo() {
        Map<Long, User> resMap = new HashMap<>();
        resMap.put(1L, new User(1L, "Sveta", false, "Kryshyna", "kalio", "ua", true, true, true));
        resMap.put(2L, new User(2L, "Jenya", false, "Stepka", "kalio", "ua", true, true, true));
        resMap.put(3L, new User(3L, "Igor", false, "Got", "kalio", "ua", true, true, true));
        resMap.put(4L, new User(4L, "Ilia", false, "Ashyn", "kalio", "ua", true, true, true));

        return resMap;
    }

    public static List<QueuePassengerDao> getTestPassengerQueue() {
        List<QueuePassengerDao> resList = new ArrayList<>();
        resList.add(new QueuePassengerDao(1, "Lavruhina", "No details"));
        resList.add(new QueuePassengerDao(2, "Drayzera", "Dve sobaki"));
//        resList.add(new QueuePassengerDao(3, "Balzaka", "Tri kota"));
//        resList.add(new QueuePassengerDao(4, "Raduzhyi", "Dvenatsat detey"));

        return resList;
    }
}
