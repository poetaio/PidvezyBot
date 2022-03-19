package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.utils.TripStatus;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "trip_id")
    private long tripId;
    @ManyToOne
    @JoinColumn(name = "passenger_id", referencedColumnName = "user_id")
    private User passenger;
    @Column(name = "address")
    private String address;
    @Column(name = "details")
    private String details;
    @Enumerated(EnumType.STRING)
    @Column(name = "trip_status")
    private TripStatus tripStatus;

    @OneToMany(mappedBy = "viewTrip")
    // add constraint when status=IN_QUEUE
    private List<User> listOfViewDriver;

    // add constraint when status=TAKEN
    @OneToOne
    @JoinColumn(name = "taken_by_driver_id", referencedColumnName = "user_id")
    private User takenByDriver;

    // add constraint when status=FINISHED
    @ManyToOne
    @JoinColumn(name = "finished_by_driver_id", referencedColumnName = "user_id")
    private User finishedByDriver;

    public Trip(User passenger) {
        this.passenger = passenger;
        tripStatus = TripStatus.INACTIVE;
    }
}
