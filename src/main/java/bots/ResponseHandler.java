package bots;

import bots.factories.SendMessageFactory;
import bots.tasks.ClearTripsAfterCurfewTask;
import bots.tasks.DriverViewUpdateTask;
import bots.utils.Constants;
import bots.utils.EmptyCallback;
import bots.utils.ResultCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import models.QueueTrip;
import models.TakenTrip;
import models.dao.LogDao;
import models.utils.State;
import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.EscapeMessageService;
import services.LogService;
import services.PersistenceService;
import services.UserService;
import services.admin_services.AdminService;
import services.driver_services.DriverService;
import services.passenger_services.NumberService;
import services.driver_services.utils.DriverUpdateEvents;
import services.trip_services.TripService;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Main class to handle all responses
 */
public class ResponseHandler {

    // TODO: REMOVE!!!
    private static ResponseHandler INSTANCE;
    private LogService logService;

    public static ResponseHandler getInstance(MessageSender sender) throws JsonProcessingException {
        if (INSTANCE == null)
            INSTANCE = new ResponseHandler(sender);
        return INSTANCE;
    }

    private final MessageSender sender;

    private UserService userService;
    private NumberService numberService;
    private DriverService driverService;
    private TripService tripService;

    private final EmptyCallback emptyCallback;
    private LogDao.LogDaoBuilder logDaoBuilder;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;

        setupServices();
        setupTasks();
        emptyCallback = new EmptyCallback();
    }

    private void setupServices() {
        logService = new LogService();

        driverService = PersistenceService.initServices(new DriverUpdateEvents() {
            @Override
            @SneakyThrows
            public void onDriverQueueEmptyEvent() {
                for (Long passengerChatId : tripService.getPassengersInQueue()) {
                    sender.executeAsync(SendMessageFactory.searchingForDriversSendMessage(passengerChatId), emptyCallback);
                }
            }

            @SneakyThrows
            @Override
            public void onDriverQueueNotEmptyEvent() {
                for (Long passengerChatId : tripService.getPassengersInQueue()) {
                    sender.executeAsync(SendMessageFactory.driversGotYourMessageSendMessage(passengerChatId), emptyCallback);
                }
            }
        });

        numberService = PersistenceService.getNumberService();
        tripService = PersistenceService.getTripService();
        userService = PersistenceService.getUserService();
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
     * @param chatId of the chat message comes from
     */
    public void replyToStart(long chatId) throws TelegramApiException {
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

        long chatId = AbilityUtils.getChatId(upd);

        if (!upd.hasMessage() || ((!upd.getMessage().hasText() || upd.getMessage().getText().indexOf('/') == 0)
                && upd.getMessage().getContact() == null)) {
            return;
        }

        String message = upd.getMessage().getText();
        SendMessage messageToSend;

        State currentState = userService.getState(chatId);
        if (currentState == null) {
            replyToStart(chatId);
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

            // Driver states
            case DRIVER_ACTIVE:
                messageToSend = onDriverActive(chatId, message);
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
                messageToSend = onEditingAddressRegular(chatId, message, upd);
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

    private SendMessage onFaq(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.faqSendMessage(chatId);
        }
    }

    private SendMessage onDriverEnteringNumber(long chatId, String message, Update upd) throws TelegramApiException {
        Contact contact = upd.getMessage().getContact();
        if (contact != null) {
            return replyToDriverEnterNumber(chatId, contact.getPhoneNumber(), upd);
        }

        switch (message) {
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverEnterNumberSendMessage(chatId);
        }
    }

    /**
     * Driver role chosen handler, subscribe to getting trip applications
     *
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
                QueueTrip driverViewTrip = tripService.getTripFromQueueByDriver(chatId);
                if (driverViewTrip == null) {
                    // sending "that trip is already taken" (when it is being approved or taken fully)
                    sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(chatId), new ResultCallback() {
                        @SneakyThrows
                        @Override
                        public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                            // TODO: move to driver service, make one method getNextTrip(chatId)
                            // move logic to services as max as possible
                            driverService.resetDriverTime(chatId);
                            QueueTrip nextTripOffer = tripService.findNextTripForDriver(chatId);
                            if (nextTripOffer == null) {
                                userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
                                sender.executeAsync(SendMessageFactory.noTripsAvailableSendMessage(chatId), emptyCallback);
                                return;
                            }
                            sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId,
                                    createDriverOfferTripMessage(nextTripOffer)), emptyCallback);
                        }
                    });
                    return null;
                }
                userService.putState(chatId, State.DRIVER_TOOK_TRIP);

                User driver = userService.getUserInfo(chatId);

                userService.putState(driverViewTrip.getPassengerChatId(), State.FOUND_A_CAR);

                sender.executeAsync(SendMessageFactory.noticingPassengerDriverTookTripSendMessage(driverViewTrip.getPassengerChatId(), driver, numberService.getNumber(chatId)), new ResultCallback() {
                    @SneakyThrows
                    @Override
                    public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                        sender.executeAsync(SendMessageFactory.askingPassengerToInformAboutTripSendMessage(driverViewTrip.getPassengerChatId()), emptyCallback);
                    }
                });

                tripService.takeDriverTrip(chatId);
                driverService.unsubscribeDriverFromUpdate(chatId);
                logDaoBuilder.putLogInfo("tripId", driverViewTrip.getTripId());

                sender.executeAsync(SendMessageFactory.driverTookTripSendMessage(chatId,
                                userService.getUserInfo(driverViewTrip.getPassengerChatId()),
                                driverViewTrip.getAddress(),
                                driverViewTrip.getDetails(),
                                numberService.getNumber(driverViewTrip.getPassengerChatId())),
                        new ResultCallback() {
                            @SneakyThrows
                            @Override
                            public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                                sender.executeAsync(SendMessageFactory.driverTookTripInformUsSendMessage(chatId), emptyCallback);
                            }
                        });
                return null;
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        createDriverOfferTripMessage(tripService.getTripFromQueueByDriver(chatId)));
        }
    }

    private SendMessage onNoTripsAvailable(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.noTripsAvailableSendMessage(chatId);
        }
    }

    /**
     * Handles State after driver takes trip, hits "Відгукнутися"
     *
     * @param chatId  driver chat id
     * @param message message sent by driver
     * @return message to reply with
     * @throws TelegramApiException basic exc
     */
    private SendMessage onDriverTookTrip(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
            case Constants.DRIVER_DISMISS_TRIP:
                TakenTrip takenTrip = tripService.getTakenTripByDriver(chatId);
                if (takenTrip != null)
                    logDaoBuilder.putLogInfo("takenTripId", takenTrip.getTripId());
                tripService.dismissDriverTrip(chatId);
                driverService.subscribeDriverOnUpdate(chatId);
                QueueTrip nextTrip = tripService.findNextTripForDriver(chatId);
                if (nextTrip == null) {
                    userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
                    return SendMessageFactory.noTripsAvailableSendMessage(chatId);
                }
                userService.putState(chatId, State.DRIVER_ACTIVE);
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        createDriverOfferTripMessage(nextTrip));
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
     * Unsubscribe from getting trip applications
     *
     * @param chatId  user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onDriverInactive(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.RESUME_BROADCAST:
                return replyToChooseRoleDriver(chatId);
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverInactiveSendMessage(chatId);
        }
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

    private SendMessage onEnteringNumber(long chatId, String message, Update upd) throws TelegramApiException {
        Contact contact = upd.getMessage().getContact();
        if (contact != null) {
            return replyToEnterNumber(chatId, contact.getPhoneNumber(), upd);
        }

        switch (message) {
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_DETAILS_REGULAR);
                return SendMessageFactory.editDetailsRegularSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                return SendMessageFactory.passengerEnterNumberSendMessage(chatId);
        }
    }

    // is active when user hits "Back" after entering details
    private SendMessage onEditingAddressRegular(long chatId, String message, Update upd) throws TelegramApiException {
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

    private SendMessage onEditingAddressApprove(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.DO_NOT_CHANGE:
                // returning to approving trip (if it's curfew time)
                return replyToEditAddressApproveNoChange(chatId, upd);
            default:
                // edit address and move back approving trip (if it's curfew time)
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEditAddressApproveNewAddress(chatId, message, upd);
                }
                // resending the same message
                return SendMessageFactory.editAddressApproveSendMessage(chatId, tripService.getTripAddress(chatId));

        }
    }

    private SendMessage onEditingAddressSearchStop(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.DO_NOT_CHANGE:
                // move back to "search stop" menu
                return replyToEditAddressSearchStopNoChange(chatId, upd);
            default:
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEditAddressSearchStopNewAddress(chatId, message, upd);
                }
                return SendMessageFactory.editAddressSearchStopSendMessage(chatId, tripService.getTripAddress(chatId));
        }
    }

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

    private SendMessage onEditingDetailsApprove(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.DO_NOT_CHANGE:
                return replyToEditDetailsApproveNoChange(chatId, upd);
            default:
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEditDetailsApproveNewDetails(chatId, message, upd);
                }
                return SendMessageFactory.editDetailsApproveSendMessage(chatId, tripService.getTripDetails(chatId));
        }
    }

    private SendMessage onEditingDetailsSearchStop(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.DO_NOT_CHANGE:
                return replyToEditDetailsSearchStopNoChange(chatId, upd);
            default:
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEditDetailsSearchStopNewDetails(chatId, message, upd);
                }
                return SendMessageFactory.editDetailsSearchStopSendMessage(chatId, tripService.getTripDetails(chatId));
        }
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
     *
     * @param chatId  passenger chat id
     * @param message message sent by passenger
     * @return message to reply with
     * @throws TelegramApiException basic exc
     */
    private SendMessage onLookingForDriver(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.STOP_LOOKING_FOR_A_CAR:
                return replyToStopLookingForACar(chatId, upd);
            default:
                return SendMessageFactory.requestPendingMessage(chatId);
        }
    }

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
     * Handles "Я знайшов транспорт" reply
     *
     * @param chatId chat id of the passenger
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
                int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
                boolean notCurfew = currentHour < Constants.CURFEW_START_HOUR && currentHour >= Constants.CURFEW_END_HOUR;
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
                        sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(driverId), new ResultCallback() {
                            @SneakyThrows
                            @Override
                            public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                                sender.execute(onChoosingRole(driverId, Constants.CHOOSE_ROLE_DRIVER));
                            }
                        });
                    }
                }
                // if driver was active, he got message that no trips available, instead of this one once again
                if (notCurfew) {
                    return createCurfewMessage(chatId, upd);
                }
                // sending a message as on trip_approved depending on number of active drivers
                userService.putState(chatId, State.LOOKING_FOR_DRIVER);
                if (driverService.getDrivers().isEmpty()) {
                    sender.executeAsync(SendMessageFactory.requestSentSendMessage(chatId), new ResultCallback() {
                        @SneakyThrows
                        @Override
                        public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                            sender.executeAsync(SendMessageFactory.searchingForDriversSendMessage(chatId), emptyCallback);
                        }
                    });
                    return null;
                }
                return SendMessageFactory.driversGotYourMessageSendMessage(chatId);
            default:
                return SendMessageFactory.askingPassengerToInformAboutTripSendMessage(chatId);
        }
    }

    // replyTo... - handlers to almost every message in every state
    private SendMessage replyToFaq(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.FAQ);
        return SendMessageFactory.faqSendMessage(chatId);
    }

    public SendMessage replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        if (userService.getUserInfo(chatId).getUserName() == null && numberService.getNumber(chatId) == null) {
            userService.putState(chatId, State.DRIVER_ENTERING_NUMBER);
            return SendMessageFactory.driverEnterNumberSendMessage(chatId);
        }

        // adding driver to both drivers list and update queue
        driverService.addDriver(chatId);

        QueueTrip tripInfo = tripService.findNextTripForDriver(chatId);

        // if no trips in queue sending corresponding message and setting corresponding state
        if (tripInfo == null) {
            userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
            return SendMessageFactory.noTripsAvailableSendMessage(chatId);
        }

        userService.putState(chatId, State.DRIVER_ACTIVE);
        return SendMessageFactory.driverActiveSendMessage(chatId, createDriverOfferTripMessage(tripInfo));
    }

    private SendMessage replyToNextTrip(long chatId) throws TelegramApiException {
        // todo: merge "reset time" with find next trip
        driverService.resetDriverTime(chatId);
        QueueTrip nextTripOffer = tripService.findNextTripForDriver(chatId);
        if (nextTripOffer == null) {
            userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
            return SendMessageFactory.noTripsAvailableSendMessage(chatId);
        }
        return SendMessageFactory.driverActiveSendMessage(chatId,
                createDriverOfferTripMessage(nextTripOffer));
    }

    public SendMessage replyToChooseRolePassenger(long chatId) throws TelegramApiException {
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
        String number = numberService.getNumber(chatId);
        if (AbilityUtils.getUser(upd).getUserName() == null && number == null) {
            userService.putState(chatId, State.ENTERING_NUMBER);
            return SendMessageFactory.passengerEnterNumberSendMessage(chatId);
        }
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }

    private SendMessage replyToDriverEnterNumber(long chatId, String number, Update upd) throws TelegramApiException {
        numberService.addNumber(chatId, number);
        // todo: move the same logic to separate method
        // todo: and call newly created method from replyToChooseRoleDriver and replyToDriverEnterNumber(this method)
        return replyToChooseRoleDriver(chatId);
    }

    private SendMessage replyToEnterNumber(long chatId, String number, Update upd) throws TelegramApiException {
        numberService.addNumber(chatId, number);
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
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
    }

    private SendMessage replyToEditDetailsRegularBack(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.EDITING_ADDRESS_REGULAR);
        return SendMessageFactory.editAddressRegularSendMessage(chatId, tripService.getTripAddress(chatId));
    }

    private SendMessage replyToEditDetailsRegularNewDetails(long chatId, String newDetails, Update upd) throws TelegramApiException {
        logOutDetailsChange(tripService.getTripId(chatId), tripService.getTripDetails(chatId), newDetails);
        tripService.setTripDetails(chatId, newDetails);
        return checkIfCurfewAndSendApproveMenu(chatId, upd);
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

    private SendMessage checkIfCurfewAndSendApproveMenu(long chatId, Update upd) throws TelegramApiException {
        if (!isNowCurfew()) {
            // changing state and "saving" trip application to send again during curfew
            userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
            return createCurfewMessage(chatId, upd);
        }
        userService.putState(chatId, State.APPROVING_TRIP);
        return createApprovingMenuMessage(chatId, upd);
    }

    private SendMessage replyToApproveTrip(long chatId, Update upd) throws TelegramApiException {
        if (!isNowCurfew()) {
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
        if (driverService.getDrivers().isEmpty()) {
            sender.executeAsync(SendMessageFactory.requestSentSendMessage(chatId), new ResultCallback() {
                @SneakyThrows
                @Override
                public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                    sender.executeAsync(SendMessageFactory.searchingForDriversSendMessage(chatId), emptyCallback);
                }
            });
            return null;
        }
        return SendMessageFactory.driversGotYourMessageSendMessage(chatId);
    }

    public SendMessage replyToStopLookingForACar(long chatId, Update upd) throws TelegramApiException {
        tripService.removeTripFromQueueByPassengerId(chatId);
        TakenTrip trip = tripService.getTakenTripByPassenger(chatId);
        if (trip != null) {
            logDaoBuilder.putLogInfo("tripId", trip.getTripId());
        }

        userService.putState(chatId, State.TRIP_SEARCH_STOPPED);
        return createSearchStopMessage(chatId, upd);
    }

    /**
     * Removes passenger trip application and sets state to creating new trip
     *
     * @param chatId chat id of the passenger
     * @return Message & menu to enter address
     */
    private SendMessage replyToEditTrip(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.EDITING_ADDRESS_REGULAR);
        tripService.removeTripFromQueueByPassengerId(chatId);
        return SendMessageFactory.editAddressRegularSendMessage(chatId, tripService.getTripAddress(chatId));
    }

    private String createDriverOfferTripMessage(@NotNull QueueTrip queueTrip) {
        User passengerInfo = userService.getUserInfo(queueTrip.getPassengerChatId());
        return EscapeMessageService.escapeMessage(Constants.IS_LOOKING_FOR_CAR_MESSAGE,
                passengerInfo.getFirstName(), passengerInfo.getLastName() != null ? " " + passengerInfo.getLastName() : "",
                queueTrip.getAddress(), queueTrip.getDetails());
    }

    private SendMessage createCurfewMessage(long chatId, Update upd) throws TelegramApiException {
        return SendMessageFactory.tryAgainDuringCurfewSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), numberService.getNumber(chatId), AbilityUtils.getUser(upd));
    }

    private SendMessage createCurfewMessage(long chatId) throws TelegramApiException {
        return SendMessageFactory.tryAgainDuringCurfewSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), numberService.getNumber(chatId), userService.getUserInfo(chatId));
    }

    private SendMessage createApprovingMenuMessage(long chatId, Update upd) throws TelegramApiException {
        return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), numberService.getNumber(chatId), AbilityUtils.getUser(upd));
    }

    private SendMessage createSearchStopMessage(long chatId, Update upd) throws TelegramApiException {
        return SendMessageFactory.tripSearchStoppedSendMessage(chatId, AbilityUtils.getUser(upd),
                tripService.getTripAddress(chatId), tripService.getTripDetails(chatId),
                numberService.getNumber(chatId));
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

    public AdminService createAdminService() {
        return new AdminService(userService, driverService, numberService, tripService);
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

    private boolean isNowCurfew() {
        int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
        if (Constants.CURFEW_START_HOUR > Constants.CURFEW_END_HOUR) {
            // if curfew starts today and ends tomorrow (22:00 - 6:00)
            return currentHour >= Constants.CURFEW_START_HOUR || currentHour < Constants.CURFEW_END_HOUR;
        } else {
            // if curfew starts today and ends today (18:00 - 23:00)
            return currentHour >= Constants.CURFEW_START_HOUR && currentHour < Constants.CURFEW_END_HOUR;
        }

    }
}
