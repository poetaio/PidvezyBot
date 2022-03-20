package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.utils.State;
import org.checkerframework.checker.units.qual.C;
import org.checkerframework.common.aliasing.qual.Unique;

import javax.persistence.*;
import java.util.List;

@Entity(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @Column(name = "user_id")
    private long userId;
    private String username;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Enumerated(EnumType.STRING)
    @Column(name = "user_state")
    private State userState;

    @ManyToOne
    @JoinColumn(name = "view_trip_id")
    private Trip viewTrip;
    @OneToOne(mappedBy = "takenByDriver")
    private Trip takenTrip;
    @OneToMany(mappedBy = "finishedByDriver")
    private List<Trip> finishedTrips;

    public User(long userId) {
        this.userId = userId;
        this.userState = State.CHOOSING_ROLE;
    }

    public User(org.telegram.telegrambots.meta.api.objects.User user) {
        this(user.getId());
        this.username = user.getUserName();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }
}
