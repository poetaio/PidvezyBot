package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.utils.TripStatus;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "trips")
public class Trip {
    @Id
    @Column(name = "trip_id")
    private UUID tripId;
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
    @ManyToOne
    @JoinColumn(name = "taken_by_driver_id", referencedColumnName = "user_id")
    private User takenByDriver;

    // add constraint when status=FINISHED
    @ManyToOne
    @JoinColumn(name = "finished_by_driver_id", referencedColumnName = "user_id")
    private User finishedByDriver;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @Column(name = "taken_at")
    private Date takenAt;

    @Column(name = "finished_at")
    private Date finishedAt;

    public Trip(User passenger) {
        this.passenger = passenger;
        tripStatus = TripStatus.INACTIVE;
    }

    public Trip(UUID tripId, User passenger) {
        this.tripId = tripId;
        this.passenger = passenger;
        tripStatus = TripStatus.INACTIVE;
    }

    public Trip(UUID tripId) {
        this.tripId = tripId;
        tripStatus = TripStatus.INACTIVE;
    }
}
