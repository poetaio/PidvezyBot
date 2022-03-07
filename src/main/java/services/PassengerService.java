package services;

import java.util.HashSet;
import java.util.Set;

public class PassengerService {
    Set<Integer> passengers;

    public PassengerService() {
        this.passengers = new HashSet<>();
    }

    public void addPassenger(int passengerId) {
        passengers.add(passengerId);
    }

    public void removePassenger(int passengerId) {
        passengers.remove(passengerId);
    }

}
