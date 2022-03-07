package models;

import lombok.*;
import models.utils.enums.DriverStatus;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Driver {
    @Id
    @GeneratedValue
    @Column(name = "driver_id")
    private int driverId;

    @Column(unique = true, nullable = false)
    private int telegramId;

    @Enumerated(EnumType.STRING)
    private DriverStatus driverStatus;

    public Driver(int telegramId) {
        this.telegramId = telegramId;
        driverStatus = DriverStatus.Free;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Driver driver = (Driver) o;
        return Objects.equals(driverId, driver.driverId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
