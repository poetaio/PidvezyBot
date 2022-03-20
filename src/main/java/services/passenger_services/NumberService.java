package services.passenger_services;

import java.util.HashMap;
import java.util.Map;

public class NumberService {
    private final Map<Long, String> usersNumbers;

    public NumberService() {
        usersNumbers = new HashMap<>();
    }

    public void addNumber(long chatId, String number) {
        usersNumbers.put(chatId, number);
    }

    public String getNumber(long chatId) {
        return usersNumbers.get(chatId);
    }
}
