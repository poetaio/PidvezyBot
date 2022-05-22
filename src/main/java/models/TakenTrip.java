package models;

import lombok.*;
import models.utils.TripStatus;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString(of = "passengerChatId")
public class TakenTrip {
    private UUID tripId;
    private long passengerChatId;
    private String address;
    private String details;
    private Long driverChatId;

    public TakenTrip(QueueTrip driverTrip, Long driverChatId) {
        tripId = driverTrip.getTripId();
        passengerChatId = driverTrip.getPassengerChatId();
        address = driverTrip.getAddress();
        details = driverTrip.getDetails();
        this.driverChatId = driverChatId;
    }

    @Override
    public TakenTrip clone() {
        return new TakenTrip(tripId, passengerChatId, address, details, driverChatId);
    }
}
