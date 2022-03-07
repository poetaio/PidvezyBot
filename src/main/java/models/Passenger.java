package models;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Passenger {
    @Id
    @GeneratedValue
    @Column(name = "passenger_id")
    private int passengerId;
    private int passengerPriority;

    public Passenger(int passengerPriority) {
        this.passengerPriority = passengerPriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Passenger passenger = (Passenger) o;
        return Objects.equals(passengerId, passenger.passengerId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
