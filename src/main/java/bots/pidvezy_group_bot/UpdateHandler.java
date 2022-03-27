package bots.pidvezy_group_bot;

import bots.pidvezy_group_bot.trip_update_handler.TripUpdateManager;
import bots.pidvezy_group_bot.utils.EmptyEditCallback;
import bots.pidvezy_group_bot.utils.SendMessageFactory;
import bots.utils.Constants;
import bots.utils.EmptyCallback;
import bots.utils.ResultCallback;
import lombok.SneakyThrows;
import models.dao.SendTripDao;
import models.hibernate.Trip;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;
import services.GroupService;
import services.event_service.EventService;
import services.event_service.utils.Events;
import services.message_services.EscapeMessageService;
import services.message_services.NormalizeService;
import services.UserService;

import java.io.Serializable;

public class UpdateHandler {
    private final MessageSender sender;
    private final Long botId;

    private final EmptyCallback emptyCallback;

    private final UserService userService;
    private final GroupService groupService;
    private final EmptyEditCallback emptyEditCallback;

    public UpdateHandler(MessageSender sender, Long botId) {
        this.sender = sender;
        this.botId = botId;

        emptyCallback = new EmptyCallback();
        emptyEditCallback = new EmptyEditCallback();
        this.userService = UserService.getInstance();
        this.groupService = GroupService.getInstance();

        TripUpdateManager updateManager = new TripUpdateManager(this::sendTripOfferMessage, this::updateTripOnFinished);
        EventService.getInstance().subscribe(Events.NEW_TRIP_EVENT, updateManager);
        EventService.getInstance().subscribe(Events.REMOVE_TRIP_EVENT, updateManager);
    }

    public void handleUpdate(Update update) throws TelegramApiException {
        Message message = update.getMessage();

        // if bot has been added send all active trips
        if (message.getNewChatMembers().stream().map(User::getId).anyMatch(x -> x.equals(botId))) {
            handleBotJoinedGroup(update);
            return;
        }

        // if bot has been deleted remove chat from updates
        if (message.getLeftChatMember() != null && message.getLeftChatMember().getId().equals(botId)) {
            handleBotLeftGroup(update);
        }
    }

    private void handleBotJoinedGroup(Update update) {
        long chatId = AbilityUtils.getChatId(update);
        String groupName = update.getMessage().getChat().getTitle();
        groupService.addNewGroup(chatId, groupName);
    }

    private void handleBotLeftGroup(Update update) {
        long chatId = AbilityUtils.getChatId(update);
        groupService.removeGroupIfActive(chatId);
    }

    @SneakyThrows
    public void sendTripOfferMessage(SendTripDao trip) {
        String tripOffer = generateTripOffer(trip);
        for (long groupId : groupService.getActiveGroupIds()) {
            sender.executeAsync(SendMessageFactory.newTripSendMessage(groupId, tripOffer), (ResultCallback) (botApiMethod, message) ->
                // save message that was sent in order to delete it after trip is finished
                    groupService.setMessageIdByGroupAndTripId(groupId, trip.getTripId(), message.getMessageId()));
//            sender.execute(SendMessageFactory.newTripSendMessage(chatId, tripOffer));
        }
    }

    @SneakyThrows
    private void updateTripOnFinished(SendTripDao trip) {
        String tripUpdatedMessage = generateTripUpdate(trip);
        for (long groupId : groupService.getActiveGroupIds()) {
            // getting trip message id to remove all info from it
            Integer messageId = groupService.getMessageIdByGroupAndTripId(groupId, trip.getTripId());
            if (messageId == null)
                continue;
            sender.executeAsync(SendMessageFactory.removeTripUpdateMessage(groupId, messageId, tripUpdatedMessage), emptyEditCallback);
        }
    }

    private String generateTripOffer(SendTripDao trip) {
        long passengerChatId = trip.getPassengerChatId();
        User user = userService.getUserInfo(passengerChatId);
        String number = NormalizeService.normalizeNumber(userService.getNumber(passengerChatId));
        String username = NormalizeService.normalizeUsername(user.getUserName(), number);

        if (user == null)
            throw new RuntimeException("No user with such id " + passengerChatId);

        return EscapeMessageService.escapeMessage(Constants.GROUP_BOT_TRIP_MESSAGE,
                user.getFirstName(), user.getLastName() == null ? "" : " " + user.getLastName(),
                trip.getAddress(), trip.getDetails(), number.isEmpty() ? "" : "\n" + number,
                username.isEmpty() ? "" : "\n" + username);
    }

    private String generateTripUpdate(SendTripDao trip) {
        long passengerChatId = trip.getPassengerChatId();
        User user = userService.getUserInfo(passengerChatId);
        String number = NormalizeService.normalizeNumber(userService.getNumber(passengerChatId));
        String username = NormalizeService.normalizeUsername(user.getUserName(), number);

        if (user == null)
            throw new RuntimeException("No user with such id " + passengerChatId);

        return EscapeMessageService.escapeMessage(Constants.GROUP_BOT_TRIP_FINISHED,
                user.getFirstName(), user.getLastName() == null ? "" : " " + user.getLastName(),
                trip.getAddress(), trip.getDetails());
    }
}
