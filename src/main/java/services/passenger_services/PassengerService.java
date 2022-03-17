package services.passenger_services;

import java.util.HashMap;
import java.util.Map;

// empty sad service
public class PassengerService {
    private final Map<Long, String> passengerNumber;

    public PassengerService() {
        passengerNumber = new HashMap<>();
    }

    public void addNumber(long chatId, String number) {
        passengerNumber.put(chatId, number);
    }

    public String getNumber(long chatId) {
        return passengerNumber.get(chatId);
    }
}
