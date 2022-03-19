package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.utils.State;
import org.checkerframework.common.aliasing.qual.Unique;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @Column(name = "user_id")
    private long userId;
    @Unique
    @Column(name = "chat_id")
    private long chatId;
    private String username;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Enumerated(EnumType.STRING)
    private State user_state;

    @ManyToOne
    private Trip viewTrip;
    @OneToMany
    private List<Trip> finishedTrips;
    @OneToOne
    private Trip takenTrip;
}
