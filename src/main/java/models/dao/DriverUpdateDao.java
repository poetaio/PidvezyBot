package models.dao;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * Contains info about driver update time.
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = "chatId")
@ToString
public class DriverUpdateDao implements Comparable<DriverUpdateDao>, Cloneable {
    private final long chatId;
    private Date nextUpdateTime;

    public DriverUpdateDao(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public int compareTo(@NotNull DriverUpdateDao o) {
        return getNextUpdateTime().compareTo(o.getNextUpdateTime());
    }

    @Override
    public DriverUpdateDao clone() {
        return new DriverUpdateDao(chatId, nextUpdateTime);
    }
}
