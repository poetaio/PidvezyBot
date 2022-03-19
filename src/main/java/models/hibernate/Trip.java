package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.utils.TripStatus;
import org.checkerframework.common.aliasing.qual.Unique;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "trip_id")
    private long tripId;
    @Unique
    @Column(name = "user_id")
    private long userId;
    @Column(name = "address")
    private String address;
    @Column(name = "details")
    private String details;
    @Enumerated(EnumType.STRING)
    private TripStatus trip_status;

    @OneToMany
    // add constraint when status=IN_QUEUE
    private List<User> listOfViewDriver;
    // add constraint when status=TAKEN
    @OneToOne
    private User takenByDriver;
    // add constraint when status=FINISHED
    @ManyToOne
    private User finishedByDriver;
}
