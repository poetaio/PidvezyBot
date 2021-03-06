package bot;

import bot.factories.SendMessageFactory;
import bot.tasks.ClearTripsAfterCurfewTask;
import bot.tasks.DriverViewUpdateTask;
import bot.trip_update_handler.TripUpdateManager;
import bot.utils.EmptyEditCallback;
import bot.utils.Constants;
import bot.utils.EmptyCallback;
import bot.utils.EmptyDeleteCallback;
import bot.utils.ResultCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import models.QueueTrip;
import models.TakenTrip;
import models.dao.SendTripDao;
import models.dao.adminDao.LogDao;
import models.hibernate.utils.GroupType;
import models.utils.State;
import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.*;
import services.driver_services.DriverService;
import services.event_service.EventService;
import services.message_services.EscapeMessageService;
import services.message_services.NormalizeService;
import services.trip_services.TripService;
import services.event_service.utils.EventListener;
import services.event_service.utils.Events;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Main class to handle all responses
 */
public class ResponseHandler implements EventListener {
    private static ResponseHandler INSTANCE;

    public static ResponseHandler getInstance(MessageSender sender, long botId) throws JsonProcessingException {
        if (INSTANCE == null)
            INSTANCE = new ResponseHandler(sender, botId);
        return INSTANCE;
    }

    private final MessageSender sender;
    private final long botId;

    private UserService userService;
    private DriverService driverService;
    private TripService tripService;
    private GroupService groupService;
    private LogService logService;

    private final EmptyCallback emptyCallback;
    private final EmptyEditCallback emptyEditCallback;
    private final EmptyDeleteCallback emptyDeleteCallback;
    private LogDao.LogDaoBuilder logDaoBuilder;

    public ResponseHandler(MessageSender sender, long botId) {
        this.sender = sender;
        this.botId = botId;

        setupServices();
        setupTasks();
        emptyCallback = new EmptyCallback();
        emptyEditCallback = new EmptyEditCallback();
        emptyDeleteCallback = new EmptyDeleteCallback();

        TripUpdateManager updateManager = new TripUpdateManager(this::sendTripOfferMessage, this::updateTripOnFinished);
        EventService.getInstance().subscribe(Events.NEW_TRIP_EVENT, updateManager);
        EventService.getInstance().subscribe(Events.REMOVE_TRIP_EVENT, updateManager);
        EventService.getInstance().subscribe(Events.DRIVER_QUEUE_NON_EMPTY_EVENT, this);
        EventService.getInstance().subscribe(Events.DRIVER_QUEUE_EMPTY_EVENT, this);
    }

    private void setupServices() {
        logService = new LogService();
        this.groupService = GroupService.getInstance();

        driverService = DriverService.getInstance();

        tripService = TripService.getInstance();
        userService = UserService.getInstance();
    }

    /**
     * Creates a thread to update all drivers' views of passengers trips
     */
    private void setupTasks() {
        new Thread(new DriverViewUpdateTask() {
            @Override
            protected void sendNoTripsAvailable(long chatId) throws TelegramApiException {
                userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
                sender.executeAsync(SendMessageFactory.noTripsAvailableSendMessage(chatId), emptyCallback);
            }

            @Override
            protected void sendTripOffer(long chatId, QueueTrip passengerDao) throws TelegramApiException {
                userService.putState(chatId, State.DRIVER_ACTIVE);
                sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId, createDriverOfferTripMessage(passengerDao)), emptyCallback);
            }

            @Override
            protected List<Long> getDriversToUpdate() {
                return driverService.getDriversToUpdate();
            }
        }).start();

        new Thread(new ClearTripsAfterCurfewTask(this::clearTripAfterCurfew)).start();
    }

    /**
     * Replies to /start command with two roles to choose from
     *
     * @param update update entity of the message
     * @param chatId of the chat message comes from
     */
    public void replyToStart(Update update, long chatId) throws TelegramApiException {
        if (update.getMessage() == null || update.getMessage().getChat() == null ||
                !"private".equals(update.getMessage().getChat().getType()))
            return;
        if (groupService.getGroupName(chatId) != null)
            return;
        userService.performCleanup(chatId);
        userService.putState(chatId, State.CHOOSING_ROLE);
        sender.executeAsync(SendMessageFactory.chooseRoleSendMessage(chatId), emptyCallback);
    }

    /**
     * Main handler, handles menu actions & states
     *
     * @param upd takes Update
     * @throws TelegramApiException Classic telegram exception
     */
    public void handleUpdate(Update upd) throws TelegramApiException {

        // if bot has been added to channel
        if (upd.getMyChatMember() != null && "administrator".equals(upd.getMyChatMember().getNewChatMember().getStatus())) {
            handleBotJoinedChannel(upd);
            return;
        }

        // if bot has been banned from channel
        if (upd.getMyChatMember() != null && ("left".equals(upd.getMyChatMember().getNewChatMember().getStatus()) ||
                "kicked".equals(upd.getMyChatMember().getNewChatMember().getStatus()))) {
            handleBotLeftChannel(upd);
            return;
        }

        if (!upd.hasMessage()) {
            return;
        }

        // if bot has been added
        if (upd.getMessage().getNewChatMembers() != null && upd.getMessage().getNewChatMembers().stream().map(User::getId).anyMatch(x -> x.equals(botId))) {
            handleBotJoinedGroup(upd);
            return;
        }

        // if bot has been deleted remove chat from updates
        if (upd.getMessage().getNewChatMembers() != null && upd.getMessage().getLeftChatMember() != null && upd.getMessage().getLeftChatMember().getId().equals(botId)) {
            handleBotLeftGroup(upd);
            return;
        }

        if (upd.getMessage() == null || upd.getMessage().getChat() == null ||
                !"private".equals(upd.getMessage().getChat().getType()))
            return;

        if (GroupService.getInstance().getGroupName(AbilityUtils.getChatId(upd)) != null)
            return;

        if ((!upd.getMessage().hasText() || upd.getMessage().getText().indexOf('/') == 0)
                && upd.getMessage().getContact() == null) {
            return;
        }

        long chatId = AbilityUtils.getChatId(upd);
        String message = upd.getMessage().getText();
        SendMessage messageToSend;

        State currentState = userService.getState(chatId);
        if (currentState == null) {
            replyToStart(upd, chatId);
            return;
        }

        logDaoBuilder = LogDao.builder();

        switch (currentState) {
            case CHOOSING_ROLE:
                userService.putUserInfo(chatId, AbilityUtils.getUser(upd));
                messageToSend = onChoosingRole(chatId, message);
                break;

            case FAQ:
                messageToSend = onFaq(chatId, message);
                break;
            case PERMIT:
                messageToSend = onPermit(chatId, message);
                break;

            // Driver states
            case DRIVER_ACTIVE:
                messageToSend = onDriverActive(chatId, message);
                break;
            case DRIVER_INACTIVE:
                messageToSend = onDriverInactive(chatId, message);
                break;
            case DRIVER_ENTERING_NUMBER:
                messageToSend = onDriverEnteringNumber(chatId, message, upd);
                break;
            case DRIVER_TOOK_TRIP:
                messageToSend = onDriverTookTrip(chatId, message);
                break;
            case NO_TRIPS_AVAILABLE:
                messageToSend = onNoTripsAvailable(chatId, message);
                break;
            case AM_GOOD_BOY:
                messageToSend = onAmGoodBoy(chatId);
                break;

            // Passenger states
            case ENTERING_ADDRESS:
                messageToSend = onEnteringAddress(chatId, message);
                break;
            case ENTERING_DETAILS:
                messageToSend = onEnteringDetails(chatId, message, upd);
                break;
            case ENTERING_NUMBER:
                messageToSend = onEnteringNumber(chatId, message, upd);
                break;
            case EDITING_ADDRESS_REGULAR:
                messageToSend = onEditingAddressRegular(chatId, message);
                break;
            case EDITING_ADDRESS_APPROVE:
                messageToSend = onEditingAddressApprove(chatId, message, upd);
                break;
            case EDITING_ADDRESS_SEARCH_STOP:
                messageToSend = onEditingAddressSearchStop(chatId, message, upd);
                break;
            case EDITING_DETAILS_REGULAR:
                messageToSend = onEditingDetailsRegular(chatId, message, upd);
                break;
            case EDITING_DETAILS_APPROVE:
                messageToSend = onEditingDetailsApprove(chatId, message, upd);
                break;
            case EDITING_DETAILS_SEARCH_STOP:
                messageToSend = onEditingDetailsSearchStop(chatId, message, upd);
                break;
            case APPROVING_TRIP:
                messageToSend = onApprovingTrip(chatId, message, upd);
                break;
            case TRY_AGAIN_DURING_CURFEW:
                messageToSend = onTryAgainDuringCurfew(chatId, message, upd);
                break;
            case LOOKING_FOR_DRIVER:
                messageToSend = onLookingForDriver(chatId, message, upd);
                break;
            case TRIP_SEARCH_STOPPED:
                messageToSend = onTripSearchStopped(chatId, message, upd);
                break;
            case FOUND_A_CAR:
                messageToSend = onFoundACar(chatId, message, upd);
                break;
            default:
                messageToSend = SendMessage.builder().chatId(String.valueOf(chatId)).text(Constants.UNKNOWN_STATE_ERROR_MESSAGE).build();
        }

        if (messageToSend != null)
            sender.execute(messageToSend);

        logDaoBuilder.stateFrom(currentState);
        logDaoBuilder.userId(chatId);
        if (currentState != State.ENTERING_NUMBER && currentState != State.DRIVER_ENTERING_NUMBER)
            logDaoBuilder.message(message);
        logDaoBuilder.stateTo(userService.getState(chatId));
        CompletableFuture.runAsync(() -> logService.createLog(logDaoBuilder.build()));
    }

    /**
     * Choosing role handler
     *
     * @param chatId  user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onChoosingRole(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.CHOOSE_ROLE_DRIVER:
                return replyToChooseRoleDriver(chatId);
            case Constants.CHOOSE_ROLE_PASSENGER:
                return replyToChooseRolePassenger(chatId);
            case Constants.FAQ:
                return replyToFaq(chatId);
            default:
                return SendMessageFactory.chooseRoleSendMessage(chatId);
        }
    }

    /**
     * Faq handler
     * @param chatId user chat id
     * @param message user message
     * @return SendMessage to send
     * @throws TelegramApiException basic exception
     */
    private SendMessage onFaq(long chatId, String message) throws TelegramApiException {
        if (Constants.BACK.equals(message)) {
            userService.putState(chatId, State.CHOOSING_ROLE);
            return SendMessageFactory.chooseRoleSendMessage(chatId);
        }
        if (Constants.HOW_TO_GET_PERMIT.equals(message)) {
            userService.putState(chatId, State.PERMIT);
            return SendMessageFactory.permitSendMessage(chatId);
        }
        return SendMessageFactory.faqSendMessage(chatId);
    }


    /**
     * Faq on driver permit handler handler
     * @param chatId user chat id
     * @param message user message
     * @return SendMessage to send
     * @throws TelegramApiException basic exception
     */
    private SendMessage onPermit(long chatId, String message) throws TelegramApiException {
        if (Constants.BACK.equals(message)) {
            userService.putState(chatId, State.FAQ);
            return SendMessageFactory.faqSendMessage(chatId);
        }
        return SendMessageFactory.permitSendMessage(chatId);
    }

    /**
     * Driver entering number handler
     * @param chatId user chat id
     * @param message user message
     * @return SendMessage to send
     * @throws TelegramApiException basic exception
     */
    private SendMessage onDriverEnteringNumber(long chatId, String message, Update upd) throws TelegramApiException {
        Contact contact = upd.getMessage().getContact();
        if (contact != null) {
            return replyToDriverEnterNumber(chatId, contact.getPhoneNumber());
        }

        if (Constants.BACK.equals(message)) {
            userService.putState(chatId, State.CHOOSING_ROLE);
            return SendMessageFactory.chooseRoleSendMessage(chatId);
        }
        return SendMessageFactory.driverEnterNumberSendMessage(chatId);
    }

    /**
     * Driver role chosen handler, subscribe to getting trip applications
     * @param chatId  user chat id
     * @param message message sent by user
     * @return Message to send to user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onDriverActive(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.NEXT_TRIP:
                return replyToNextTrip(chatId);
            case Constants.TAKE_TRIP:
                return replyToDriverActiveTookTrip(chatId);
            // Unsubscribe from getting trip applications
            case Constants.STOP_BROADCAST:
                return replyToDriverActiveStopBroadcast(chatId);
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        createDriverOfferTripMessage(tripService.getTripFromQueueByDriver(chatId)));
        }
    }

    /**
     * Driver stopped broadcast handler.
     *
     * @param chatId  user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onDriverInactive(long chatId, String message) throws TelegramApiException {
        if (Constants.RESUME_BROADCAST.equals(message)) {
            return replyToDriverInactiveResumeBroadcast(chatId);
        }

        return SendMessageFactory.driverInactiveSendMessage(chatId);
    }


    /**
     * Handles "No trips available state" for driver
     *
     * @param chatId  user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onNoTripsAvailable(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            // Unsubscribe from getting trip applications
            case Constants.STOP_BROADCAST:
                return replyToNoTripAvailableStopBroadcast(chatId);
            default:
                return SendMessageFactory.noTripsAvailableSendMessage(chatId);
        }
    }

    /**
     * Handles driver State after taking trip
     *
     * @param chatId  driver chat id
     * @param message message sent by driver
     * @return message to reply with
     * @throws TelegramApiException basic exc
     */
    private SendMessage onDriverTookTrip(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.DRIVER_DISMISS_TRIP:
                return replyToDriverTookTripDismiss(chatId);
            case Constants.FINISH_TRIP:
                userService.putState(chatId, State.AM_GOOD_BOY);
                return SendMessageFactory.goodBoySendMessage(chatId);
            default:
                return SendMessageFactory.approveOrDismissTrip(chatId);
        }
    }

    private SendMessage onAmGoodBoy(long chatId) throws TelegramApiException {
        return replyToChooseRoleDriver(chatId);
    }

    /**
     * Entering passenger destination address handler,
     * if entered passenger address is valid saves it for future trip
     *
     * @param chatId  user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringAddress(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            userService.putState(chatId, State.CHOOSING_ROLE);
            return SendMessageFactory.chooseRoleSendMessage(chatId);
        } else if (!message.isEmpty() && !message.isBlank()) {
            return replyToEnterAddress(chatId, message);
        } else {
            return SendMessageFactory.enterAddressSendMessage(chatId);
        }
    }

    /**
     * Entering passenger destination address handler,
     * if entered passenger details are valid saves them for future trip
     *
     * @param chatId  user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringDetails(long chatId, String message, Update upd) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            userService.putState(chatId, State.EDITING_ADDRESS_REGULAR);
            return SendMessageFactory.editAddressRegularSendMessage(chatId, tripService.getTripAddress(chatId));
        } else if (!message.isEmpty() && !message.isBlank()) {
            return replyToEnterDetails(chatId, message, upd);
        } else {
            return SendMessageFactory.enterDetailsSendMessage(chatId);
        }
    }


    /**
     * Entering passenger user number if no username provided handler
     * @param chatId  user chat id
     * @param message message sent by user
     * @param upd Update entity
     * @return SendMessage to send
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringNumber(long chatId, String message, Update upd) throws TelegramApiException {
        Contact contact = upd.getMessage().getContact();
        if (contact != null) {
            return replyToEnterNumber(chatId, contact.getPhoneNumber(), upd);
        }

        if (Constants.BACK.equals(message)) {
            userService.putState(chatId, State.EDITING_DETAILS_REGULAR);
            return SendMessageFactory.editDetailsRegularSendMessage(chatId, tripService.getTripDetails(chatId));
        }

        return SendMessageFactory.passengerEnterNumberSendMessage(chatId);
    }

    /**
     * Handles editing address when user hits "Back" after entering/editing details "for the first time"
     * @param chatId
     * @param message
     * @return SendMessage to send
     * @throws TelegramApiException
     */
    private SendMessage onEditingAddressRegular(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.DO_NOT_CHANGE:
                // moving to entering/editing details
                return replyToEditAddressRegularNoChange(chatId);
            case Constants.BACK:
                // returning to choosing role
                return replyToEditAddressRegularBack(chatId);
            default:
                // if the message is not empty and not blank, consider it as the new address
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEditAddressRegularNewAddress(chatId, message);
                }
                return SendMessageFactory.editAddressRegularSendMessage(chatId, tripService.getTripAddress(chatId));
        }
    }

    /**
     * Handles editing address on trip approving
     * @param chatId
     * @param message
     * @return SendMessage to send
     * @throws TelegramApiException
     */
    private SendMessage onEditingAddressApprove(long chatId, String message, Update upd) throws TelegramApiException {
        if (Constants.DO_NOT_CHANGE.equals(message)) {// returning to approving trip (if it's curfew time)
            return replyToEditAddressApproveNoChange(chatId, upd);
        }

        // edit address and move back approving trip (if it's curfew time)
        if (!message.isEmpty() && !message.isBlank()) {
            return replyToEditAddressApproveNewAddress(chatId, message, upd);
        }

        // resending the same message
        return SendMessageFactory.editAddressApproveSendMessage(chatId, tripService.getTripAddress(chatId));
    }
    /**
     * Handles editing address on trip search stopped
     * @param chatId
     * @param message
     * @return SendMessage to send
     * @throws TelegramApiException
     */
    private SendMessage onEditingAddressSearchStop(long chatId, String message, Update upd) throws TelegramApiException {
        if (Constants.DO_NOT_CHANGE.equals(message)) {// move back to "search stop" menu
            return replyToEditAddressSearchStopNoChange(chatId, upd);
        }

        if (!message.isEmpty() && !message.isBlank()) {
            return replyToEditAddressSearchStopNewAddress(chatId, message, upd);
        }

        return SendMessageFactory.editAddressSearchStopSendMessage(chatId, tripService.getTripAddress(chatId));
    }
    /**
     * Handles editing details on "Back" button on "Entering number"/Approving trip
     * @param chatId user chat id
     * @param message sent message by user
     * @return SendMessage to send
     * @throws TelegramApiException basic exc
     */
    private SendMessage onEditingDetailsRegular(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.DO_NOT_CHANGE:
                // move to trip approving
                return replyToEditDetailsRegularNoChange(chatId, upd);
            case Constants.BACK:
                // move to editing address
                return replyToEditDetailsRegularBack(chatId);
            default:
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEditDetailsRegularNewDetails(chatId, message, upd);
                }
                return SendMessageFactory.editDetailsRegularSendMessage(chatId, tripService.getTripDetails(chatId));
        }
    }

    /**
     * Handles editing details on trip approving
     * @param chatId chat id of the user
     * @param message message sent
     * @param upd Update entity
     * @return SendMessage to send
     * @throws TelegramApiException basic exception
     */
    private SendMessage onEditingDetailsApprove(long chatId, String message, Update upd) throws TelegramApiException {
        if (Constants.DO_NOT_CHANGE.equals(message)) {
            return replyToEditDetailsApproveNoChange(chatId, upd);
        }

        if (!message.isEmpty() && !message.isBlank()) {
            return replyToEditDetailsApproveNewDetails(chatId, message, upd);
        }

        return SendMessageFactory.editDetailsApproveSendMessage(chatId, tripService.getTripDetails(chatId));
    }

    /**
     * Handles editing details on trip search stopped
     * @param chatId chat id of the user
     * @param message message sent
     * @param upd Update entity
     * @return SendMessage to send
     * @throws TelegramApiException basic exception
     */
    private SendMessage onEditingDetailsSearchStop(long chatId, String message, Update upd) throws TelegramApiException {
        if (Constants.DO_NOT_CHANGE.equals(message)) {
            return replyToEditDetailsSearchStopNoChange(chatId, upd);
        }

        if (!message.isEmpty() && !message.isBlank()) {
            return replyToEditDetailsSearchStopNewDetails(chatId, message, upd);
        }

        return SendMessageFactory.editDetailsSearchStopSendMessage(chatId, tripService.getTripDetails(chatId));
    }

    /**
     * Handles state when passenger is approving trip
     *
     * @param chatId  passenger chat id
     * @param message message sent by passenger
     * @param upd     Update entity containing info
     * @return message to reply with
     * @throws TelegramApiException basic exc
     */
    private SendMessage onApprovingTrip(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.APPROVE_TRIP:
                return replyToApproveTrip(chatId, upd);
            case Constants.EDIT_ADDRESS:
                userService.putState(chatId, State.EDITING_ADDRESS_APPROVE);
                return SendMessageFactory.editAddressApproveSendMessage(chatId, tripService.getTripAddress(chatId));
            case Constants.EDIT_DETAILS:
                userService.putState(chatId, State.EDITING_DETAILS_APPROVE);
                return SendMessageFactory.editDetailsApproveSendMessage(chatId, tripService.getTripDetails(chatId));
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_DETAILS_REGULAR);
                return SendMessageFactory.editDetailsRegularSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                return createApprovingMenuMessage(chatId, upd);
        }
    }

    /**
     * Handles block screen when it's not curfew time
     * @param chatId chat id of the user
     * @param message message sent
     * @param upd Update entity
     * @return SendMessage to send
     * @throws TelegramApiException basic exception
     */
    private SendMessage onTryAgainDuringCurfew(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.TRY_AGAIN:
                return replyToApproveTrip(chatId, upd);
            case Constants.EDIT_ADDRESS:
                userService.putState(chatId, State.EDITING_ADDRESS_APPROVE);
                return SendMessageFactory.editAddressApproveSendMessage(chatId, tripService.getTripAddress(chatId));
            case Constants.EDIT_DETAILS:
                userService.putState(chatId, State.EDITING_DETAILS_APPROVE);
                return SendMessageFactory.editDetailsApproveSendMessage(chatId, tripService.getTripDetails(chatId));
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_DETAILS_REGULAR);
                return SendMessageFactory.editDetailsRegularSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                return createCurfewMessage(chatId, upd);
        }
    }

    /**
     * Handles state when passenger has approved trip and is looking for driver
     * @param chatId  passenger chat id
     * @param message message sent by passenger
     * @return message to reply with
     * @throws TelegramApiException basic exc
     */
    private SendMessage onLookingForDriver(long chatId, String message, Update upd) throws TelegramApiException {
        if (Constants.STOP_LOOKING_FOR_A_CAR.equals(message)) {
            return replyToStopLookingForACar(chatId, upd);
        }

        return SendMessageFactory.requestPendingMessage(chatId);
    }

    /**
     * Handles state when passenger stopped trip search
     * @param chatId chat id
     * @param message message
     * @param upd Update entity
     * @return SendMessage to send
     * @throws TelegramApiException basic exception
     */
    private SendMessage onTripSearchStopped(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.RESUME_SEARCH:
                return replyToApproveTrip(chatId, upd);
            case Constants.CANCEL_TRIP:
                tripService.cancelTripOnSearchStopped(chatId);
                userService.putState(chatId, State.CHOOSING_ROLE);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            case Constants.EDIT_ADDRESS:
                userService.putState(chatId, State.EDITING_ADDRESS_SEARCH_STOP);
                return SendMessageFactory.editAddressSearchStopSendMessage(chatId, tripService.getTripAddress(chatId));
            case Constants.EDIT_DETAILS:
                userService.putState(chatId, State.EDITING_DETAILS_SEARCH_STOP);
                return SendMessageFactory.editDetailsSearchStopSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                return createSearchStopMessage(chatId, upd);
        }
    }

    /**
     * Handles user state, when driver took trip reply
     * @param chatId chat id of the passenger
     * @param  message sent by user
     * @param chatId user chat id
     * @return Choosing role text & menu
     * @throws TelegramApiException basic telegram exception
     */
    private SendMessage onFoundACar(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.FOUND_TRIP:
                tripService.removeTripOnPassengerFoundACar(chatId);
                userService.putState(chatId, State.CHOOSING_ROLE);
                TakenTrip trip = tripService.getTakenTripByPassenger(chatId);
                if (trip != null)
                    logDaoBuilder.putLogInfo("tripId", trip.getTripId());
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            case Constants.FIND_AGAIN:
                boolean notCurfew = !CurfewService.isNowCurfew();
                // removing from taken and adding to queue
                // checking if driver still views this trip (to send him message that it's already unavailable)
                TakenTrip currentTrip = tripService.getTakenTripByPassenger(chatId);
                tripService.dismissPassengerTrip(chatId);
                // firstly removing trip from queue then sending a driver a new trip,
                // so that driver does not see the same trip, when in should've been deleted
                if (notCurfew) {
                    userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
                    tripService.removeTripFromQueueByPassengerId(chatId);
                }
                Long driverId = currentTrip.getDriverChatId();
                if (driverId != null) {
                    State driverState = userService.getState(driverId);
                    tripService.dismissDriverTrip(driverId);
                    if (driverState == State.DRIVER_TOOK_TRIP) {
                        sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(driverId),
                                (ResultCallback) (botApiMethod, message1) ->
                                        sendDriverNextTripOfferAsync(driverId));
                    }
                }
                // if driver was active, he got message that no trips available, instead of this one once again
                if (notCurfew) {
                    return createCurfewMessage(chatId, upd);
                }
                // sending a message as on trip_approved depending on number of active drivers
                userService.putState(chatId, State.LOOKING_FOR_DRIVER);
                return sendLookingForDriverOrDriversGotYourMessage(chatId);
            default:
                return SendMessageFactory.askingPassengerToInformAboutTripSendMessage(chatId);
        }
    }

    // replyTo... - handlers to almost every message in every state
    private SendMessage replyToFaq(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.FAQ);
        return SendMessageFactory.faqSendMessage(chatId);
    }

    private SendMessage replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        if (userService.getUserInfo(chatId).getUserName() == null && userService.getNumber(chatId) == null) {
            userService.putState(chatId, State.DRIVER_ENTERING_NUMBER);
            return SendMessageFactory.driverEnterNumberSendMessage(chatId);
        }

        // adding driver to both drivers list and update queue
        driverService.addDriver(chatId);
        return subscribeDriverOnUpdateAndSendNextTripOffer(chatId);
    }

    private SendMessage replyToDriverEnterNumber(long chatId, String number) throws TelegramApiException {
        userService.addNumber(chatId, number);
        return replyToChooseRoleDriver(chatId);
    }

    private SendMessage replyToNextTrip(long chatId) throws TelegramApiException {
        driverService.resetDriverTime(chatId);
        QueueTrip nextTripOffer = tripService.findNextTripForDriver(chatId);
        if (nextTripOffer == null) {
            userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
            return SendMessageFactory.noTripsAvailableSendMessage(chatId);
        }
        return SendMessageFactory.driverActiveSendMessage(chatId,
                createDriverOfferTripMessage(nextTripOffer));
    }

    private SendMessage replyToDriverActiveStopBroadcast(long chatId) throws TelegramApiException {
        return stopBroadcastForDriver(chatId);
    }

    private SendMessage replyToDriverActiveTookTrip(long chatId) throws TelegramApiException {
        QueueTrip driverViewTrip = tripService.getTripFromQueueByDriver(chatId);
        if (driverViewTrip == null) {
            // sending "that trip is already taken" (when it is being approved or taken fully)
            sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(chatId),
                    (ResultCallback) (botApiMethod, message1) ->
                            resetDriverAndSendNextTripOfferAsync(chatId)
            );
            return null;
        }
        userService.putState(chatId, State.DRIVER_TOOK_TRIP);

        User driver = userService.getUserInfo(chatId);

        userService.putState(driverViewTrip.getPassengerChatId(), State.FOUND_A_CAR);

        sender.executeAsync(SendMessageFactory.noticingPassengerDriverTookTripSendMessage(driverViewTrip.getPassengerChatId(),
                driver, userService.getNumber(chatId)), (ResultCallback) (botApiMethod, message12) ->
                askPassengerToInformAboutTripEnd(driverViewTrip.getPassengerChatId()));

        tripService.takeDriverTrip(chatId);
        driverService.unsubscribeDriverFromUpdate(chatId);
        logDaoBuilder.putLogInfo("tripId", driverViewTrip.getTripId());

        sender.executeAsync(SendMessageFactory.driverTookTripSendMessage(chatId,
                        userService.getUserInfo(driverViewTrip.getPassengerChatId()),
                        driverViewTrip.getAddress(),
                        driverViewTrip.getDetails(),
                        userService.getNumber(driverViewTrip.getPassengerChatId())),
                (ResultCallback) (botApiMethod, message13) ->
                        sendDriverInformUsAboutTripAsync(chatId));
        return null;
    }

    private SendMessage replyToNoTripAvailableStopBroadcast(long chatId) throws TelegramApiException {
        return stopBroadcastForDriver(chatId);
    }

    private SendMessage replyToDriverTookTripDismiss(long chatId) throws TelegramApiException {
        TakenTrip takenTrip = tripService.getTakenTripByDriver(chatId);
        if (takenTrip != null)
            logDaoBuilder.putLogInfo("takenTripId", takenTrip.getTripId());
        tripService.dismissDriverTrip(chatId);
        driverService.subscribeDriverOnUpdate(chatId);

        return sendDriverNextTripOffer(chatId);
    }

    private SendMessage replyToDriverInactiveResumeBroadcast(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.DRIVER_ACTIVE);
        return subscribeDriverOnUpdateAndSendNextTripOffer(chatId);
    }

    private SendMessage replyToChooseRolePassenger(long chatId) throws TelegramApiException {
        String currentAddress = tripService.getTripAddress(chatId);
        if (currentAddress != null) {
            userService.putState(chatId, State.EDITING_ADDRESS_REGULAR);
            return SendMessageFactory.editAddressRegularSendMessage(chatId, currentAddress);
        }
        userService.putState(chatId, State.ENTERING_ADDRESS);
        return SendMessageFactory.enterAddressSendMessage(chatId);
    }

    private SendMessage replyToEnterAddress(long chatId, String newAddress) throws TelegramApiException {
        logOutAddressChange(tripService.getTripId(chatId), newAddress, tripService.getTripAddress(chatId));
        tripService.setTripAddress(chatId, newAddress);
        userService.putState(chatId, State.ENTERING_DETAILS);
        return SendMessageFactory.enterDetailsSendMessage(chatId);
    }

    private SendMessage replyToEnterDetails(long chatId, String newDetails, Update upd) throws TelegramApiException {
        logOutDetailsChange(tripService.getTripId(chatId), newDetails, tripService.getTripDetails(chatId));
        tripService.setTripDetails(chatId, newDetails);
        return checkPhoneCurfewAndSendApproveMenu(chatId, upd);
    }

    private SendMessage replyToEnterNumber(long chatId, String number, Update upd) throws TelegramApiException {
        userService.addNumber(chatId, number);
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }

    // editing address regular (after hitting "Back" on regular details, or after choosing role menu)
    private SendMessage replyToEditAddressRegularNewAddress(long chatId, String newAddress) throws TelegramApiException {
        logOutAddressChange(tripService.getTripId(chatId), tripService.getTripAddress(chatId), newAddress);
        tripService.setTripAddress(chatId, newAddress);
        return checkIfDetailsPresentSendDetailsMenu(chatId);
    }

    private SendMessage replyToEditAddressRegularNoChange(long chatId) throws TelegramApiException {
        return checkIfDetailsPresentSendDetailsMenu(chatId);
    }

    private SendMessage replyToEditAddressRegularBack(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.CHOOSING_ROLE);
        return SendMessageFactory.chooseRoleSendMessage(chatId);
    }

    private SendMessage checkIfDetailsPresentSendDetailsMenu(long chatId) throws TelegramApiException {
        // if details were already entered, they can be edited
        String currentDetails = tripService.getTripDetails(chatId);
        if (currentDetails == null) {
            userService.putState(chatId, State.ENTERING_DETAILS);
            return SendMessageFactory.enterDetailsSendMessage(chatId);
        }
        userService.putState(chatId, State.EDITING_DETAILS_REGULAR);
        return SendMessageFactory.editDetailsRegularSendMessage(chatId, currentDetails);
    }

    // editing address on approve menu replies
    private SendMessage replyToEditAddressApproveNewAddress(long chatId, String newAddress, Update upd) throws TelegramApiException {
        logOutAddressChange(tripService.getTripId(chatId), tripService.getTripAddress(chatId), newAddress);
        // set new address, check if no curfew, and return to approving trip
        tripService.setTripAddress(chatId, newAddress);
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }

    private SendMessage replyToEditAddressApproveNoChange(long chatId, Update upd) throws TelegramApiException {
        // check if no curfew and return to approving trip
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }

    // editing address on "Stop trip search" menu

    private SendMessage replyToEditAddressSearchStopNewAddress(long chatId, String newAddress, Update upd) throws TelegramApiException {
        logOutAddressChange(tripService.getTripId(chatId), tripService.getTripAddress(chatId), newAddress);
        tripService.setTripAddress(chatId, newAddress);
        userService.putState(chatId, State.TRIP_SEARCH_STOPPED);
        return createSearchStopMessage(chatId, upd);
    }
    private SendMessage replyToEditAddressSearchStopNoChange(long chatId, Update upd) throws TelegramApiException {
        userService.putState(chatId, State.TRIP_SEARCH_STOPPED);
        return createSearchStopMessage(chatId, upd);
    }

    // edit details regular replies
    private SendMessage replyToEditDetailsRegularNoChange(long chatId, Update upd) throws TelegramApiException {
        return checkPhoneCurfewAndSendApproveMenu(chatId, upd);
    }

    private SendMessage replyToEditDetailsRegularBack(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.EDITING_ADDRESS_REGULAR);
        return SendMessageFactory.editAddressRegularSendMessage(chatId, tripService.getTripAddress(chatId));
    }

    private SendMessage replyToEditDetailsRegularNewDetails(long chatId, String newDetails, Update upd) throws TelegramApiException {
        logOutDetailsChange(tripService.getTripId(chatId), tripService.getTripDetails(chatId), newDetails);
        tripService.setTripDetails(chatId, newDetails);
        return checkPhoneCurfewAndSendApproveMenu(chatId, upd);
    }

    // edit details "Approve trip" menu replies
    private SendMessage replyToEditDetailsApproveNoChange(long chatId, Update upd) throws TelegramApiException {
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }

    private SendMessage replyToEditDetailsApproveNewDetails(long chatId, String newDetails, Update upd) throws TelegramApiException {
        logOutDetailsChange(tripService.getTripId(chatId), tripService.getTripDetails(chatId), newDetails);
        tripService.setTripDetails(chatId, newDetails);
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }


    // edit details "Trip search stopped" menu replies
    private SendMessage replyToEditDetailsSearchStopNoChange(long chatId, Update upd) throws TelegramApiException {
        userService.putState(chatId, State.TRIP_SEARCH_STOPPED);
        return createSearchStopMessage(chatId, upd);
    }

    private SendMessage replyToEditDetailsSearchStopNewDetails(long chatId, String newDetails, Update upd) throws TelegramApiException {
        logOutDetailsChange(tripService.getTripId(chatId), tripService.getTripDetails(chatId), newDetails);
        userService.putState(chatId, State.TRIP_SEARCH_STOPPED);
        tripService.setTripDetails(chatId, newDetails);
        return createSearchStopMessage(chatId, upd);
    }

    private SendMessage stopBroadcastForDriver(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.DRIVER_INACTIVE);
        driverService.unsubscribeDriverFromUpdate(chatId);
        return SendMessageFactory.driverInactiveSendMessage(chatId);
    }

    private SendMessage checkPhoneCurfewAndSendApproveMenu(long chatId, Update upd) throws TelegramApiException {
        String number = userService.getNumber(chatId);
        if (AbilityUtils.getUser(upd).getUserName() == null && number == null) {
            userService.putState(chatId, State.ENTERING_NUMBER);
            return SendMessageFactory.passengerEnterNumberSendMessage(chatId);
        }

        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }

    private SendMessage checkIfCurfewAndSendApproveMenu(long chatId, Update upd) throws TelegramApiException {
        if (!CurfewService.isNowCurfew()) {
            // changing state and "saving" trip application to send again during curfew
            userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
            return createCurfewMessage(chatId, upd);
        }
        userService.putState(chatId, State.APPROVING_TRIP);
        return createApprovingMenuMessage(chatId, upd);
    }

    private SendMessage replyToApproveTrip(long chatId, Update upd) throws TelegramApiException {
        if (!CurfewService.isNowCurfew()) {
            // changing state and "saving" trip application to send again during curfew
            userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
            return createCurfewMessage(chatId, upd);
        }

        userService.putState(chatId, State.LOOKING_FOR_DRIVER);
        tripService.addNewTripToQueue(chatId);
        QueueTrip trip = tripService.getTripFromQueueByPassenger(chatId);
        logDaoBuilder.putLogInfo("tripId", trip.getTripId());
        logDaoBuilder.putLogInfo("address", trip.getAddress());
        logDaoBuilder.putLogInfo("details", trip.getDetails());

        return sendLookingForDriverOrDriversGotYourMessage(chatId);
    }

    private SendMessage replyToStopLookingForACar(long chatId, Update upd) throws TelegramApiException {
        tripService.removeTripFromQueueByPassengerId(chatId);
        TakenTrip trip = tripService.getTakenTripByPassenger(chatId);
        if (trip != null) {
            logDaoBuilder.putLogInfo("tripId", trip.getTripId());
        }

        userService.putState(chatId, State.TRIP_SEARCH_STOPPED);
        return createSearchStopMessage(chatId, upd);
    }

    private String createDriverOfferTripMessage(@NotNull QueueTrip queueTrip) {
        User passengerInfo = userService.getUserInfo(queueTrip.getPassengerChatId());
        return EscapeMessageService.escapeMessageWithArgs(Constants.IS_LOOKING_FOR_CAR_MESSAGE,
                passengerInfo.getFirstName(), passengerInfo.getLastName() != null ? " " + passengerInfo.getLastName() : "",
                queueTrip.getAddress(), queueTrip.getDetails());
    }

    private SendMessage createCurfewMessage(long chatId, Update upd) throws TelegramApiException {
        return SendMessageFactory.tryAgainDuringCurfewSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), userService.getNumber(chatId), AbilityUtils.getUser(upd));
    }

    private SendMessage createCurfewMessage(long chatId) throws TelegramApiException {
        return SendMessageFactory.tryAgainDuringCurfewSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), userService.getNumber(chatId), userService.getUserInfo(chatId));
    }

    private SendMessage createApprovingMenuMessage(long chatId, Update upd) throws TelegramApiException {
        return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), userService.getNumber(chatId), AbilityUtils.getUser(upd));
    }

    private SendMessage createSearchStopMessage(long chatId, Update upd) throws TelegramApiException {
        return SendMessageFactory.tripSearchStoppedSendMessage(chatId, AbilityUtils.getUser(upd),
                tripService.getTripAddress(chatId), tripService.getTripDetails(chatId),
                userService.getNumber(chatId));
    }

    private void logOutAddressChange(UUID tripId, String oldAddress, String newAddress) {
        logDaoBuilder.putLogInfo("tripId", tripId);
        logDaoBuilder.putLogInfo("newAddress", oldAddress);
        logDaoBuilder.putLogInfo("oldAddress", newAddress);
    }

    private void logOutDetailsChange(UUID tripId, String oldDetails, String newDetails) {
        logDaoBuilder.putLogInfo("tripId", tripId);
        logDaoBuilder.putLogInfo("oldDetails", oldDetails);
        logDaoBuilder.putLogInfo("newDetails", newDetails);
    }

    @SneakyThrows
    private void clearTripAfterCurfew() {
        List <Long> passengersInQueue = tripService.getAllQueueTrips()
                .stream()
                .map(QueueTrip::getPassengerChatId)
                .collect(Collectors.toList());

        for (long chatId : passengersInQueue) {
            userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
            tripService.removeTripFromQueueByPassengerId(chatId);
            sender.executeAsync(SendMessageFactory.curfewIsOverSendMessage(chatId),
                (ResultCallback) (botApiMethod, message) ->
                        sendTryAgainDuringCurfewMessage(chatId));
        }
    }

    @SneakyThrows
    private void sendTryAgainDuringCurfewMessage(long chatId) {
        sender.executeAsync(createCurfewMessage(chatId), emptyCallback);
    }

    @SneakyThrows
    private void resetDriverAndSendNextTripOfferAsync(long chatId) {
        driverService.resetDriverTime(chatId);
        QueueTrip nextTripOffer = tripService.findNextTripForDriver(chatId);
        if (nextTripOffer == null) {
            userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
            sender.executeAsync(SendMessageFactory.noTripsAvailableSendMessage(chatId), emptyCallback);
            return;
        }
        userService.putState(chatId, State.DRIVER_ACTIVE);
        sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId,
                createDriverOfferTripMessage(nextTripOffer)), emptyCallback);
    }

    private SendMessage subscribeDriverOnUpdateAndSendNextTripOffer(long chatId) throws TelegramApiException {
        driverService.subscribeDriverOnUpdate(chatId);
        return sendDriverNextTripOffer(chatId);
    }

    private SendMessage sendDriverNextTripOffer(Long chatId) throws TelegramApiException {
        QueueTrip nextTrip = tripService.findNextTripForDriver(chatId);

        // if no trips in queue sending corresponding message and setting corresponding state
        if (nextTrip == null) {
            userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
            return SendMessageFactory.noTripsAvailableSendMessage(chatId);
        }

        userService.putState(chatId, State.DRIVER_ACTIVE);
        return SendMessageFactory.driverActiveSendMessage(chatId,
                createDriverOfferTripMessage(nextTrip));
    }

    @SneakyThrows
    private void sendDriverNextTripOfferAsync(Long chatId) {
        sender.executeAsync(sendDriverNextTripOffer(chatId), emptyCallback);
    }

    private SendMessage sendLookingForDriverOrDriversGotYourMessage(long chatId) throws TelegramApiException {
        if (driverService.getDrivers().isEmpty()) {
            sender.executeAsync(SendMessageFactory.requestSentSendMessage(chatId),
                    (ResultCallback) (botApiMethod, message) -> sendSearchingForDriversAsync(chatId));
            return null;
        }
        return SendMessageFactory.driversGotYourMessageSendMessage(chatId);
    }

    @SneakyThrows
    private void sendSearchingForDriversAsync(long chatId) {
        sender.executeAsync(SendMessageFactory.searchingForDriversSendMessage(chatId), emptyCallback);
    }

    @SneakyThrows
    private void askPassengerToInformAboutTripEnd(long passengerChatId) {
        sender.executeAsync(SendMessageFactory.askingPassengerToInformAboutTripSendMessage(passengerChatId), emptyCallback);
    }

    @SneakyThrows
    private void sendDriverInformUsAboutTripAsync(long chatId) {
        sender.executeAsync(SendMessageFactory.driverTookTripInformUsSendMessage(chatId), emptyCallback);
    }

    @Override
    public void update(String eventName, Object o) {
        if (Objects.equals(eventName, Events.DRIVER_QUEUE_EMPTY_EVENT))
            onDriverQueueEmptyEvent();
        else if (Objects.equals(eventName, Events.DRIVER_QUEUE_NON_EMPTY_EVENT))
            onDriverQueueNotEmptyEvent();
    }

    @SneakyThrows
    private void onDriverQueueEmptyEvent() {
        for (Long passengerChatId : tripService.getPassengersInQueue()) {
            sender.executeAsync(SendMessageFactory.searchingForDriversSendMessage(passengerChatId), emptyCallback);
        }
    }

    @SneakyThrows
    public void onDriverQueueNotEmptyEvent() {
        for (Long passengerChatId : tripService.getPassengersInQueue()) {
            sender.executeAsync(SendMessageFactory.driversGotYourMessageSendMessage(passengerChatId), emptyCallback);
        }
    }

    private void handleBotJoinedGroup(Update update) {
        long chatId = AbilityUtils.getChatId(update);
        String groupName = update.getMessage().getChat().getTitle();
        groupService.addNewGroup(chatId, groupName);
    }

    private void handleBotLeftGroup(Update update) {
        long chatId = AbilityUtils.getChatId(update);
        groupService.removeIfActive(chatId);
    }

    private void handleBotJoinedChannel(Update update) {
        long channelId = AbilityUtils.getChatId(update);
        String channelName = update.getMyChatMember().getChat().getTitle();
        groupService.addNewChannel(channelId, channelName);
    }

    private void handleBotLeftChannel(Update update) {
        long channelId = AbilityUtils.getChatId(update);
        groupService.removeIfActive(channelId);
    }

    @SneakyThrows
    public void sendTripOfferMessage(SendTripDao trip) {
        String tripOffer = generateTripOffer(trip);
        for (long groupId : groupService.getActiveGroupIds()) {
            sender.executeAsync(bot.utils.SendMessageFactory.newTripSendMessage(groupId, tripOffer), (ResultCallback) (botApiMethod, message) ->
                    // save message that was sent in order to delete it after trip is finished
                    groupService.setMessageIdByGroupAndTripId(groupId, trip.getTripId(), message.getMessageId()));
        }
    }

    @SneakyThrows
    private void updateTripOnFinished(SendTripDao trip) {
        if (trip == null)
            return;

        String tripUpdatedMessage = generateTripUpdate(trip);

        for (long groupId : groupService.getActiveGroupIds()) {
            // getting trip message id to remove all info from it
            Integer messageId = groupService.getMessageIdByGroupAndTripId(groupId, trip.getTripId());
            groupService.deleteMessage(groupId, trip.getTripId());

            if (messageId == null) {
                continue;
            }

            GroupType groupType = groupService.getGroupType(groupId);

            // if group   -> update
            // if channel -> delete
            if (Objects.equals(groupType, GroupType.GROUP)) {
                sender.executeAsync(bot.utils.SendMessageFactory.removeTripUpdateMessage(groupId, messageId, tripUpdatedMessage), emptyEditCallback);
            }
            else if (Objects.equals(groupType, GroupType.CHANNEL)) {
                sender.executeAsync(bot.utils.SendMessageFactory.removeTripDeleteMessage(groupId, messageId), emptyDeleteCallback);
            }
        }
    }

    private String generateTripOffer(SendTripDao trip) {
        long passengerChatId = trip.getPassengerChatId();
        User user = userService.getUserInfo(passengerChatId);
        if (user == null)
            throw new RuntimeException("No user with such id " + passengerChatId);

        String number = NormalizeService.normalizeNumber(userService.getNumber(passengerChatId));
        String username = NormalizeService.normalizeUsername(user.getUserName(), number);

        return EscapeMessageService.escapeMessageWithArgs(Constants.GROUP_BOT_TRIP_MESSAGE,
                user.getFirstName(), user.getLastName() == null ? "" : " " + user.getLastName(),
                trip.getAddress(), trip.getDetails(), number.isEmpty() ? "" : "\n" + number,
                username.isEmpty() ? "" : "\n" + username);
    }

    private String generateTripUpdate(SendTripDao trip) {
        long passengerChatId = trip.getPassengerChatId();
        User user = userService.getUserInfo(passengerChatId);

        if (user == null)
            throw new RuntimeException("No user with such id " + passengerChatId);

        return EscapeMessageService.escapeMessageWithArgs(Constants.GROUP_BOT_TRIP_FINISHED,
                user.getFirstName(), user.getLastName() == null ? "" : " " + user.getLastName(),
                trip.getAddress(), trip.getDetails());
    }
}
