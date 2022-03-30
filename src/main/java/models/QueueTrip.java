package models;

import lombok.*;
import models.hibernate.Trip;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = "passengerChatId")
@ToString(of = "passengerChatId")
public class QueueTrip {
    private UUID tripId;
    private long passengerChatId;
    private String address;
    private String details;
    private List<Long> driverList = new LinkedList<>();
    private Date lastViewDate;

    public QueueTrip() {
        tripId = UUID.randomUUID();
    }

    public QueueTrip(long passengerChatId, String address, String details) {
        this();
        this.passengerChatId = passengerChatId;
        this.address = address;
        this.details = details;
    }

    public QueueTrip(UUID tripId, long passengerChatId, String address, String details) {
        this.tripId = tripId;
        this.passengerChatId = passengerChatId;
        this.address = address;
        this.details = details;
    }

    public QueueTrip(long passengerChatId) {
        this(passengerChatId, null, null);
    }

    public QueueTrip(TakenTrip takenTrip) {
        this(takenTrip.getTripId(), takenTrip.getPassengerChatId(), takenTrip.getAddress(), takenTrip.getDetails());
    }

    public QueueTrip(Trip trip) {
        this(trip.getTripId(), trip.getPassenger().getUserId(), trip.getAddress(), trip.getDetails());
    }

    public void addDriverChatId(long driverChatId) {
        lastViewDate = new Date();
        this.driverList.add(driverChatId);
    }

    public void removeDriverChatId(long driverChatId) {
        this.driverList.remove(driverChatId);
    }

    public boolean isViewedByDriver(long driverChatId) {
        return driverList.contains(driverChatId);
    }

    public int getDriverCount() {
        return driverList.size();
    }

    @Override
    public QueueTrip clone() {
        return new QueueTrip(tripId, passengerChatId, address, details,
                driverList, lastViewDate);
    }
}
