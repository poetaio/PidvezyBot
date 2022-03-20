package services.trip_services;

import models.TakenTrip;
import repositories.TripRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Trip gets here when driver looks details and takes it
 */
public class TakenTripService {
    private List<TakenTrip> takenTrips;
    private final TripRepository tripRepository;

    public TakenTripService(List<TakenTrip> takenTrips) {
        this.takenTrips = takenTrips;
        tripRepository = new TripRepository();
    }

    /**
     * When driver takes it from the view menu (to approve in future)
     * @param takenTrip trip
     */
    public void addTakenTrip(TakenTrip takenTrip) {
        takenTrips.add(0, takenTrip);
    }

    public void dismissDriverTrip(long driverChatId) {
        TakenTrip trip = getTripByDriverChatId(driverChatId);
        if (trip != null) {
            trip.setDriverChatId(null);
            CompletableFuture.runAsync(() -> tripRepository.unsetDriverTookTrip(trip.getTripId(), driverChatId));
        }
    }

    public TakenTrip getTripByPassengerChatId(long passengerChatId) {
        Optional<TakenTrip> tripRes = takenTrips.stream().filter(
                x -> x.getPassengerChatId() == passengerChatId
        ).findFirst();
        return tripRes.orElse(null);
    }

    public TakenTrip getTripByDriverChatId(long driverChatId) {
        Optional<TakenTrip> tripRes = takenTrips.stream().filter(
                x -> x.getDriverChatId() == driverChatId
        ).findFirst();
        return tripRes.orElse(null);
    }

    public void removeTrip(long driverChatId) {
        takenTrips = takenTrips.stream().filter(
                x -> x.getDriverChatId() != driverChatId
        ).collect(Collectors.toList());
    }

    public void removePassengerTrip(long passengerChatId) {
        takenTrips = takenTrips.stream().filter(
                x -> x.getPassengerChatId() != passengerChatId
        ).collect(Collectors.toList());
    }

    public TakenTrip getAndRemoveTripByDriverChatId(long driverChatId) {
        TakenTrip trip = getTripByDriverChatId(driverChatId);
        takenTrips = takenTrips.stream().filter(x -> x.getDriverChatId() != driverChatId).collect(Collectors.toList());
        return trip;
    }

    public TakenTrip getAndRemoveTripByPassengerChatId(long passengerChatId) {
        TakenTrip trip = getTripByPassengerChatId(passengerChatId);
        takenTrips = takenTrips.stream()
                .filter(x -> x.getPassengerChatId() != passengerChatId)
                .collect(Collectors.toList());
        return trip;
    }

    public List<TakenTrip> getAll() {
        return takenTrips.stream().map(TakenTrip::clone).collect(Collectors.toList());
    }
}
