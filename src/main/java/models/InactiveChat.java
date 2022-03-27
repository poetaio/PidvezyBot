package models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class InactiveChat {
    private long chatId;
    private String chatName;

    @Override
    public InactiveChat clone() {
        return new InactiveChat(chatId, chatName);
    }
}
