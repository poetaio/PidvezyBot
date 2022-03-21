package services.passenger_services;

import java.util.HashMap;
import java.util.Map;

public class NumberService {
    private final Map<Long, String> usersNumbers;

    public NumberService(Map<Long, String> usersNumbers) {
        this.usersNumbers = usersNumbers;
    }

    public void addNumber(long chatId, String number) {
        usersNumbers.put(chatId, number);
    }

    public String getNumber(long chatId) {
        return usersNumbers.get(chatId);
    }
}
