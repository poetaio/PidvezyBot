package bots;

import bots.utils.Constants;
import bots.utils.PassengerState;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.MessageSender;

import java.util.Map;

public class PassengerResponseHandler {
    private final MessageSender sender;
    private final Map<Long, PassengerState> passengerChatStates;

    public PassengerResponseHandler(MessageSender sender, DBContext db) {
        this.sender = sender;
        passengerChatStates = db.getMap(Constants.PASSENGER_STATES);
    }
}
