package models.hibernate;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity(name = "sent_message")
@Getter
@Setter
public class LogMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "sent_message_id")
    private long sentMessageId;

    @Column(name = "user_id")
    private long userId;
    private byte[] message;
    @Column(name = "message_time")
    private Timestamp messageTime;

    public LogMessage() {
        messageTime = new Timestamp(System.currentTimeMillis());
    }

    public LogMessage(long chatId, byte[] messageText) {
        this();
        this.message = messageText;
        this.userId = chatId;
    }
}
