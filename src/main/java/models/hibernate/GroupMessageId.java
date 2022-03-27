package models.hibernate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GroupMessageId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long groupId;
    private UUID tripId;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result
                + ((tripId == null) ? 0 : tripId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupMessageId that = (GroupMessageId) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(tripId, that.tripId);
    }
}
