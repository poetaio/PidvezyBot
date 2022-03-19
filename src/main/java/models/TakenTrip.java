package models;

import lombok.*;
import models.utils.TripStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString(of = "passengerChatId")
public class TakenTrip {
    private long passengerChatId;
    private String address;
    private String details;
    private TripStatus tripStatus;
    private long driverChatId;

    public TakenTrip(QueueTrip driverTrip, long driverChatId) {
        passengerChatId = driverTrip.getPassengerChatId();
        address = driverTrip.getAddress();
        details = driverTrip.getDetails();
        this.driverChatId = driverChatId;
        tripStatus = TripStatus.TAKEN;
    }

    @Override
    public TakenTrip clone() {
        return new TakenTrip(passengerChatId, address, details, tripStatus, driverChatId);
    }
}
