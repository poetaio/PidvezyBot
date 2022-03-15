package models;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString(of = "passengerChatId")
public class Trip {
    private long passengerChatId;
    private String address;
    private String details;
    private Long driverChatId;

    public Trip(long passengerChatId, String address, String details) {
        this.passengerChatId = passengerChatId;
        this.address = address;
        this.details = details;
    }

    @Override
    public Trip clone() {
        return new Trip(passengerChatId, address, details, driverChatId);
    }
}
