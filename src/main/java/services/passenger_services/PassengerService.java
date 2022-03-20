package services.passenger_services;

import repositories.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// empty sad service
public class PassengerService {
    private final Map<Long, String> passengerNumberMap;
    private final UserRepository userRepository;

    public PassengerService(Map<Long, String> passengerNumberMap) {
        this.passengerNumberMap = passengerNumberMap;
        userRepository = new UserRepository();
    }

    public void addNumber(long chatId, String number) {
        passengerNumberMap.put(chatId, number);
        CompletableFuture.runAsync(() -> userRepository.setNumber(chatId, number));
    }

    public String getNumber(long chatId) {
        return passengerNumberMap.get(chatId);
    }
}
