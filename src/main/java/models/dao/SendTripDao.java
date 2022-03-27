package models.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.QueueTrip;
import models.TakenTrip;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class SendTripDao {
    private UUID tripId;
    private long passengerChatId;
    private String address;
    private String details;

    public SendTripDao(TakenTrip o) {
        tripId = o.getTripId();
        passengerChatId = o.getPassengerChatId();
        address = o.getAddress();
        details = o.getDetails();
    }

    public SendTripDao(QueueTrip o) {
        tripId = o.getTripId();
        passengerChatId = o.getPassengerChatId();
        address = o.getAddress();
        details = o.getDetails();
    }
}
