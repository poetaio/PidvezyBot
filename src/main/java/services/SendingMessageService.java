package services;

// Contains single object to synchronize on while sending and deleting message in chat
public class SendingMessageService {
    public static final Object MessageSending = new Object();
}
