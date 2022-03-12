package models.dao;

import lombok.*;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueuePassengerDao {
    private long passengerChatId;
    private String address;
    private String details;
    private Long driverChatId;

    public QueuePassengerDao(long passengerChatId, String address, String details) {
        this.passengerChatId = passengerChatId;
        this.address = address;
        this.details = details;
    }

    @Override
    public QueuePassengerDao clone() {
        return new QueuePassengerDao(passengerChatId, address, details, driverChatId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != QueuePassengerDao.class) return false;
        QueuePassengerDao that = (QueuePassengerDao) o;
        return that.getPassengerChatId() == getPassengerChatId() &&
                that.getAddress().equals(getAddress()) &&
                that.getDetails().equals(getDetails()) &&
                that.getDriverChatId().equals(getDriverChatId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(passengerChatId, address, details, driverChatId);
    }

    @Override
    public String toString() {
        return "QueuePassengerDao{" +
                "passengerChatId=" + passengerChatId +
                '}';
    }
}
