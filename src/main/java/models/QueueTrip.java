package models;

import lombok.*;
import models.utils.TripStatus;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "passengerChatId")
@ToString(of = "passengerChatId")
public class QueueTrip {
    private long passengerChatId;
    private String address;
    private String details;
    private List<Long> driverList;
    private Date lastViewDate;

    public QueueTrip(long passengerChatId, String address, String details) {
        this.passengerChatId = passengerChatId;
        this.address = address;
        this.details = details;
        driverList = new LinkedList<>();
    }

    public QueueTrip(long passengerChatId) {
        this.passengerChatId = passengerChatId;
    }

    public QueueTrip(TakenTrip takenTrip) {
        this(takenTrip.getPassengerChatId(), takenTrip.getAddress(), takenTrip.getDetails());
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
        return new QueueTrip(passengerChatId, address, details,
                driverList, lastViewDate);
    }
}
