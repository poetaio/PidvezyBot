package services.passenger_services;

import repositories.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NumberService {
    private final Map<Long, String> usersNumbers;
    private final UserRepository userRepository;

    public NumberService(Map<Long, String> usersNumbers) {
        this.usersNumbers = usersNumbers;
        userRepository = new UserRepository();
    }

    public void addNumber(long chatId, String number) {
        usersNumbers.put(chatId, number);
        CompletableFuture.runAsync(() -> userRepository.setNumber(chatId, number));
    }

    public String getNumber(long chatId) {
        return usersNumbers.get(chatId);
    }
}
