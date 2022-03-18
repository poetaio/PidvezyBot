package services.trip_services;

import models.TakenTrip;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In future matches corresponding SQL table
 */
public class FinishedTripService {
    private final List<TakenTrip> finishedTrips;

    public FinishedTripService() {
        finishedTrips = new LinkedList<>();
    }

    public void addFinishedTrip(TakenTrip takenTrip) {
        finishedTrips.add(takenTrip);
    }

    public List<TakenTrip> getFinishedTripsByDriverId(long driverChatId) {
        return finishedTrips.stream()
                .filter(x -> x.getDriverChatId() == driverChatId)
                .collect(Collectors.toList());
    }

    public List<TakenTrip> getAll() {
        return finishedTrips.stream()
                .map(TakenTrip::clone)
                .collect(Collectors.toList());
    }
}
