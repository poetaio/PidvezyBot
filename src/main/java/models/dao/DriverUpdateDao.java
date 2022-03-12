package models.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
public class DriverUpdateDao implements Comparable<DriverUpdateDao>, Cloneable {
    private final long chatId;
    private Date nextUpdateTime;

    public DriverUpdateDao(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        DriverUpdateDao that = (DriverUpdateDao) o;
        return that.getChatId() == getChatId();
    }

    @Override
    public int compareTo(@NotNull DriverUpdateDao o) {
        return getNextUpdateTime().compareTo(o.getNextUpdateTime());
    }

    @Override
    public String toString() {
        return "chatId=" + chatId + " time: " + getNextUpdateTime();
    }

    @Override
    public DriverUpdateDao clone() {
        return new DriverUpdateDao(chatId, nextUpdateTime);
    }
}
