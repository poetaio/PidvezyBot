package services;

import java.util.HashSet;
import java.util.Set;

public class DriverService {

    Set<Integer> driversSet;

    public DriverService() {
        driversSet = new HashSet<>();
    }

    public void addDriver(int driverId) {
        driversSet.add(driverId);
    }

    public void removeDriver(int driverId) {
        driversSet.remove(driverId);
    }
}
