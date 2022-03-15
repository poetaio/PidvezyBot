package models.hibernate_not_being_used;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
//import models.utils.enums.TripStatus;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Trip {
    @Id
    @GeneratedValue
    private int tripId;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "passenger_id")
    private Passenger passengerId;
//    @Enumerated(EnumType.STRING)
//    private TripStatus status;

    public Trip(Passenger passengerId) {
        this.passengerId = passengerId;
//        status = TripStatus.LookingForDriver;
    }
}
