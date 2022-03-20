package services.trip_services;

import models.QueueTrip;
import models.TakenTrip;
import repositories.TripRepository;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * In future matches corresponding SQL table
 */
public class FinishedTripService {
    private final List<TakenTrip> finishedTrips;
    private final TripRepository tripRepository;

    public FinishedTripService(List<TakenTrip> finishedTrips) {
        this.finishedTrips = finishedTrips;
        tripRepository = new TripRepository();
    }

    public void addFinishedTrip(TakenTrip takenTrip) {
        finishedTrips.add(takenTrip);
        CompletableFuture.runAsync(() -> tripRepository.finishTrip(takenTrip.getTripId()));
    }

    public void addFinishedTrip(QueueTrip trip) {
        finishedTrips.add(new TakenTrip(trip, null));
        CompletableFuture.runAsync(() -> tripRepository.finishTrip(trip.getTripId()));
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
