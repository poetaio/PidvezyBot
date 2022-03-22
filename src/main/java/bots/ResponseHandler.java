package bots;

import bots.factories.ReplyMarkupFactory;
import bots.factories.SendMessageFactory;
import bots.tasks.ClearTripsAfterCurfewTask;
import bots.tasks.DriverViewUpdateTask;
import bots.tasks.utils.ClearCallback;
import bots.utils.Constants;
import bots.utils.EmptyCallback;
import bots.utils.ResultCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import models.QueueTrip;
import models.TakenTrip;
import models.utils.State;
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
import java.util.stream.Collectors;

/**
 * Main class to handle all responses
 */
public class ResponseHandler {

    // TODO: REMOVE!!!
    private static ResponseHandler INSTANCE;

    public static ResponseHandler getInstance(MessageSender sender) throws JsonProcessingException {
        if (INSTANCE == null)
            INSTANCE = new ResponseHandler(sender);
        return INSTANCE;
    }

    private final MessageSender sender;

    private final UserService userService;
    private final NumberService numberService;
    private final DriverService driverService;
    private final TripService tripService;

    private final EmptyCallback emptyCallback;

    public ResponseHandler(MessageSender sender) throws JsonProcessingException {
        this.sender = sender;

        driverService = PersistenceService.initServices(new DriverUpdateEvents() {
//        numberService = new NumberService();
//        driverService = new DriverService() {
            @Override
            @SneakyThrows
            public void onDriverQueueEmptyEvent() {
                for (Long passengerChatId : tripService.getPassengersInQueue()) {
//                    sender.executeAsync(SendMessageFactory.requestSentSendMessage(passengerChatId), new ResultCallback() {
//                        @SneakyThrows
//                        @Override
//                        public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
//                            sender.executeAsync(SendMessageFactory.searchingForDriversSendMessage(passengerChatId), emptyCallback);
//                        }
//                    });
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
//        userService = new UserService(driverService, tripService);

//        UsersInitializer.parseDrivers(driverService, userService);
//        UsersInitializer.parseInactiveTrips(tripService, tripService.getTripBuilderService(), userService);
//        UsersInitializer.parseQueueTrip(tripService, tripService.getTripBuilderService(), userService);
//        UsersInitializer.parseTakenTrips(tripService, tripService.getTripBuilderService(), userService);

        emptyCallback = new EmptyCallback();

        setupTasks();
    }

    /**
     * Creates a thread to update all drivers' views of passengers trips
     */
    private void setupTasks() {
        new Thread(new DriverViewUpdateTask() {
            @Override
            protected void sendNoTripsAvailable(long chatId) throws TelegramApiException {
                userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
                sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE), emptyCallback);
            }

            @Override
            protected void sendTripOffer(long chatId, QueueTrip passengerDao) throws TelegramApiException {
                userService.putState(chatId, State.DRIVER_ACTIVE);
                sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId, generateDriverOfferTripMessage(chatId, passengerDao)), emptyCallback);
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

        switch (currentState) {
            case CHOOSING_ROLE:
                userService.putUserInfo(chatId, AbilityUtils.getUser(upd));
                messageToSend = onChoosingRole(chatId, message);
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
            case DRIVER_IN_TRIP:
                messageToSend = onDriverInTrip(chatId, message);
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
            case EDITING_ADDRESS:
                messageToSend = onEditingAddress(chatId, message, upd);
                break;
            case EDITING_DETAILS:
                messageToSend = onEditingDetails(chatId, message, upd);
                break;
            case APPROVING_TRIP:
                messageToSend = onApprovingTrip(chatId, message, upd);
                break;
            case TRY_AGAIN_DURING_CURFEW:
                messageToSend = onTryAgainDuringCurfew(chatId, message, upd);
                break;
            case LOOKING_FOR_DRIVER:
                messageToSend = onLookingForDriver(chatId, message);
                break;
            case TRIP_SEARCH_STOPPED:
                messageToSend = onTripSearchStopped(chatId, message, upd);
                break;
            case FOUND_A_CAR:
                messageToSend = onFoundACar(chatId, message, upd);
                break;
            case THANKS:
                messageToSend = onThanks(chatId);
                break;
            default:
                messageToSend = SendMessage.builder().chatId(String.valueOf(chatId)).text(Constants.UNKNOWN_STATE_ERROR_MESSAGE).build();
        }

//        sender.executeAsync(messageToSend, emptyCallback);
        // to see errors in logs
        if (messageToSend != null)
            sender.execute(messageToSend);
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
            default:
                return SendMessageFactory.chooseRoleSendMessage(chatId);
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
                return SendMessageFactory.enterNumberSendMessage(chatId);
        }
    }

    /**
     * Driver role chosen handler, subscribe to getting trip applications
     *
     * @param chatId  user chat Id
     * @param message message sent by user
     * @return Message to send to user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onDriverActive(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.NEXT_TRIP:
                // TODO: move to driver service, make one method getNextTrip(chatId)
                // move logic to services as max as possible
                driverService.resetDriverTime(chatId);
                QueueTrip nextPassenger = tripService.findNextTripForDriver(chatId);
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(chatId, nextPassenger));
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
                            QueueTrip nextPassenger1 = tripService.findNextTripForDriver(chatId);
                            if (nextPassenger1 == null) {
                                userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
                                sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId,
                                        Constants.NO_TRIPS_MESSAGE), emptyCallback);
                            } else {
                                sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId,
                                        generateDriverOfferTripMessage(chatId, nextPassenger1)), emptyCallback);
                            }
                        }
                    });
                    return null;
                }
                userService.putState(chatId, State.DRIVER_TOOK_TRIP);

                User driver = userService.getUserInfo(chatId);

                userService.putState(driverViewTrip.getPassengerChatId(), State.FOUND_A_CAR);

//                sender.executeAsync(SendMessageFactory.noticingPassengerDriverTookTripSendMessage(driverViewTrip.getPassengerChatId(), driver),  emptyCallback);
//                sender.executeAsync(SendMessageFactory.askingPassengerToInformAboutTripSendMessage(driverViewTrip.getPassengerChatId()),  emptyCallback);

                sender.executeAsync(SendMessageFactory.noticingPassengerDriverTookTripSendMessage(driverViewTrip.getPassengerChatId(), driver, numberService.getNumber(chatId)), new ResultCallback() {
                    @SneakyThrows
                    @Override
                    public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                        sender.executeAsync(SendMessageFactory.askingPassengerToInformAboutTripSendMessage(driverViewTrip.getPassengerChatId()), emptyCallback);
                    }
                });

                tripService.takeDriverTrip(chatId);
                driverService.unsubscribeDriverFromUpdate(chatId);

                return SendMessageFactory.driverTookTripSendMessage(chatId,
                        userService.getUserInfo(driverViewTrip.getPassengerChatId()),
                        driverViewTrip.getAddress(),
                        driverViewTrip.getDetails(),
                        numberService.getNumber(driverViewTrip.getPassengerChatId()));
//                        passengerService.getNumber(driverViewTrip.getPassengerChatId()));

            // TODO: NULLPOINTER CHECK
                // TODO: NULLPOINTER CHECK
//                return SendMessageFactory.askingDriverToInformAboutEndOfTripSendMessage(chatId);
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(chatId, tripService.getTripFromQueueByDriver(chatId)));
        }
    }

    private SendMessage onNoTripsAvailable(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE);
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
//                sendNotificationToDrivers(chatId, false);
                tripService.dismissDriverTrip(chatId);
                driverService.subscribeDriverOnUpdate(chatId);
                QueueTrip nextTrip = tripService.findNextTripForDriver(chatId);
                if (nextTrip == null) {
                    userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
                    return SendMessageFactory.driverActiveSendMessage(chatId,
                            Constants.NO_TRIPS_MESSAGE);
                }
                userService.putState(chatId, State.DRIVER_ACTIVE);
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(chatId, nextTrip));
            case Constants.DRIVER_PIDVEZY:
//                if (currentTrip == null) {
//                    sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(chatId), emptyCallback);
//                    return replyToChooseRoleDriver(chatId);
//                TakenTrip currentTrip = tripService.getTakenTripByDriver(chatId);
//                TakenTrip finishedTrip = tripService.getFinishedTripByDriver(chatId);
//                if (currentTrip == null) {
//                    sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(chatId), new ResultCallback() {
//                        @SneakyThrows
//                        @Override
//                        public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
//                            sender.executeAsync(replyToChooseRoleDriver(chatId), emptyCallback);
//                        }
//                    });
//                    return null;
//                }
                userService.putState(chatId, State.DRIVER_IN_TRIP);
                return SendMessageFactory.askingDriverToInformAboutEndOfTripSendMessage(chatId);
            default:
                TakenTrip driverPassenger = tripService.getTakenTripByDriver(chatId);
//                if (driverPassenger == null) {
//                    driverService.subscribeDriverOnUpdate(chatId);
//                    QueueTrip nextTrip1 = tripService.findNextTripForDriver(chatId);
//                    if (nextTrip1 == null) {
//                        userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
//                        return SendMessageFactory.driverActiveSendMessage(chatId,
//                                Constants.NO_TRIPS_MESSAGE);
//                    }
//                    userService.putState(chatId, State.DRIVER_ACTIVE);
//                    return SendMessageFactory.driverActiveSendMessage(chatId,
//                            generateDriverOfferTripMessage(chatId, nextTrip1));
//                }
                // TODO: NULLPOINTER CHECK userInfo.get and passengerQueueService.getByDriver
//                return SendMessageFactory.driverTookTripSendMessage(chatId, userService.getUserInfo(
//                                tripService.getTakenTripByDriver(chatId).getPassengerChatId()),
//                        driverPassenger.getAddress(),
//                        driverPassenger.getDetails(),
//                        numberService.getNumber(driverPassenger.getPassengerChatId()));
                return SendMessageFactory.approveOrDismissTrip(chatId);
        }
    }

    private SendMessage onDriverInTrip(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.FINISH_TRIP:
//                TakenTrip currentTrip2 = tripService.getTakenTripByDriver(chatId);
//                if (currentTrip2 == null) {
//                    sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(chatId), new ResultCallback() {
//                        @SneakyThrows
//                        @Override
//                        public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
//                            sender.executeAsync(replyToChooseRoleDriver(chatId), emptyCallback);
//                        }
//                    });
//                    return null;
//                }
                userService.putState(chatId, State.AM_GOOD_BOY);
                return SendMessageFactory.goodBoySendMessage(chatId);
//            case Constants.BACK:
//                QueueTrip driverPassenger = tripService.getQueueTripByDriver(chatId);
//                if (driverPassenger == null) {
//                    driverService.subscribeDriverOnUpdate(chatId);
//                    QueueTrip nextTrip1 = tripService.findNextQueueTrip(chatId);
//                    if (nextTrip1 == null) {
//                        userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
//                        return SendMessageFactory.driverActiveSendMessage(chatId,
//                                Constants.NO_TRIPS_MESSAGE);
//                    }
//                    userService.putState(chatId, State.DRIVER_ACTIVE);
//                    return SendMessageFactory.driverActiveSendMessage(chatId,
//                            generateDriverOfferTripMessage(chatId, nextTrip1));
//                }
//                // TODO: NULLPOINTER CHECK userInfo.get and passengerQueueService.getByDriver
//                return SendMessageFactory.driverTookTripSendMessage(chatId, userService.getUserInfo(
//                                tripService.getQueueTripByDriver(chatId).getPassengerChatId()),
//                        driverPassenger.getAddress(),
//                        driverPassenger.getDetails(),
//                        passengerService.getNumber(driverPassenger.getPassengerChatId()));
            default:
//                TakenTrip currentTrip1 = tripService.getTakenTripByDriver(chatId);
//                TakenTrip finishedTrip = tripService.getFinishedTripByDriver(chatId);
//                if (currentTrip1 == null) {
//                    sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(chatId), new ResultCallback() {
//                        @SneakyThrows
//                        @Override
//                        public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
//                            sender.executeAsync(replyToChooseRoleDriver(chatId), emptyCallback);
//                        }
//                    });
//                    return null;
//                }
                return SendMessageFactory.askingDriverToInformAboutEndOfTripSendMessage(chatId);
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
            userService.putState(chatId, State.EDITING_ADDRESS);
            return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
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
                userService.putState(chatId, State.EDITING_DETAILS);
                return SendMessageFactory.editDetailsSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                return SendMessageFactory.enterNumberSendMessage(chatId);
        }
    }

    private SendMessage onEditingAddress(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                // todo: refactor
                userService.putState(chatId, State.CHOOSING_ROLE);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
//                return replyToEnterDetails(chatId, tripService.getTripDetails(chatId), upd);
            case Constants.DO_NOT_CHANGE:
                String currentDetails = tripService.getTripDetails(chatId);
                if (currentDetails == null) {
                    userService.putState(chatId, State.EDITING_DETAILS);
                    return SendMessageFactory.enterDetailsSendMessage(chatId);
                }
                userService.putState(chatId, State.EDITING_DETAILS);
                return SendMessageFactory.editDetailsSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEditAddress(chatId, message);
                } else {
                    return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
                }
        }
    }

    private SendMessage onEditingDetails(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.DO_NOT_CHANGE:
                return replyToEnterDetails(chatId, tripService.getTripDetails(chatId), upd);
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_ADDRESS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
            default:
                if (!message.isEmpty() && !message.isBlank()) {
                    return replyToEnterDetails(chatId, message, upd);
                } else {
                    return SendMessageFactory.editDetailsSendMessage(chatId, tripService.getTripDetails(chatId));
                }
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
            case Constants.CHANGE_TRIP_INFO:
                userService.putState(chatId, State.EDITING_ADDRESS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_DETAILS);
                return SendMessageFactory.editDetailsSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                        tripService.getTripDetails(chatId), numberService.getNumber(chatId), AbilityUtils.getUser(upd));
        }
    }

    private SendMessage onTryAgainDuringCurfew(long chatId, String message, Update upd) throws TelegramApiException {
        int currentHour;
        switch (message) {
            case Constants.TRY_AGAIN:
                return replyToApproveTrip(chatId, upd);
            case Constants.CHANGE_TRIP_INFO:
                userService.putState(chatId, State.EDITING_ADDRESS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_DETAILS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
                if (currentHour >= Constants.CURFEW_START_HOUR || currentHour < Constants.CURFEW_END_HOUR) {
                    userService.putState(chatId, State.APPROVING_TRIP);
                }
                return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                        tripService.getTripDetails(chatId), numberService.getNumber(chatId), AbilityUtils.getUser(upd));

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
    private SendMessage onLookingForDriver(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.EDIT_TRIP:
                return replyToEditTrip(chatId);
            case Constants.STOP_LOOKING_FOR_A_CAR:
                return replyToStopLookingForACar(chatId);
            default:
                return replyWithText(chatId, Constants.REQUEST_PENDING_MESSAGE);
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
            case Constants.CHANGE_TRIP_INFO:
                userService.putState(chatId, State.EDITING_ADDRESS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
            default:
                return replyToStopLookingForACar(chatId);
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
                userService.putState(chatId, State.THANKS);
                return SendMessageFactory.wishAGoodTripSendMessage(chatId);
            case Constants.FIND_AGAIN:
                int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
                boolean notCurfew = currentHour < Constants.CURFEW_START_HOUR && currentHour >= Constants.CURFEW_END_HOUR;
                // removing from taken and adding to queue
                // checking if driver still views this trip (to send him message that it's already unavailable)
                TakenTrip currentTrip = tripService.getTakenTripByPassenger(chatId);
                tripService.dismissPassengerTrip(chatId);
                // firstly removing trip from queue then sending a driver a new trip
                if (notCurfew) {
                    userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
                    tripService.removeTripFromQueueByPassengerId(chatId);
                }
                Long driverId = currentTrip.getDriverChatId();
                if (driverId != null) {
                    State driverState = userService.getState(driverId);
                    tripService.dismissDriverTrip(driverId);
                    if (driverState == State.DRIVER_IN_TRIP || driverState == State.DRIVER_TOOK_TRIP) {
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
                    return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                            tripService.getTripDetails(chatId), numberService.getNumber(chatId), AbilityUtils.getUser(upd));
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

    private SendMessage onThanks(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.CHOOSING_ROLE);
        return SendMessageFactory.chooseRoleSendMessage(chatId);
    }

    // replyTo... - handlers to almost every message in every state
    public SendMessage replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        if (userService.getUserInfo(chatId).getUserName() == null && numberService.getNumber(chatId) == null) {
            userService.putState(chatId, State.DRIVER_ENTERING_NUMBER);
            return SendMessageFactory.enterNumberSendMessage(chatId);
        }

        // adding driver to both drivers list and update queue
        driverService.addDriver(chatId);

        QueueTrip tripInfo = tripService.findNextTripForDriver(chatId);

        if (tripInfo == null) {
            userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
            return SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE);
        }

        userService.putState(chatId, State.DRIVER_ACTIVE);
        // todo: merge with generateDriverOfferTrip
        User passengerUserInfo = userService.getUserInfo(tripInfo.getPassengerChatId());
        String message = EscapeMessageService.escapeMessage(Constants.IS_LOOKING_FOR_CAR_MESSAGE,
                passengerUserInfo.getFirstName(), passengerUserInfo.getLastName() != null ? " " + passengerUserInfo.getLastName() : "",
                tripInfo.getAddress(), tripInfo.getDetails());
        return SendMessageFactory.driverActiveSendMessage(chatId, message);
    }

    public SendMessage replyToChooseRolePassenger(long chatId) throws TelegramApiException {
        String currentAddress = tripService.getTripAddress(chatId);
        if (currentAddress != null) {
            userService.putState(chatId, State.EDITING_ADDRESS);
            return SendMessageFactory.editAddressSendMessage(chatId, currentAddress);
        }
        userService.putState(chatId, State.ENTERING_ADDRESS);
        return SendMessageFactory.enterAddressSendMessage(chatId);
    }

    private SendMessage replyToEnterAddress(long chatId, String address) throws TelegramApiException {
        tripService.setTripAddress(chatId, address);
        userService.putState(chatId, State.ENTERING_DETAILS);
        return SendMessageFactory.enterDetailsSendMessage(chatId);
    }

    private SendMessage replyToEnterDetails(long chatId, String details, Update upd) throws TelegramApiException {
        tripService.setTripDetails(chatId, details);
        String number = numberService.getNumber(chatId);
        if (number != null) {
            int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
            if (currentHour >= Constants.CURFEW_START_HOUR || currentHour < Constants.CURFEW_END_HOUR) {
                userService.putState(chatId, State.APPROVING_TRIP);
            } else {
                userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
            }
            return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                    tripService.getTripDetails(chatId), number, AbilityUtils.getUser(upd));
        } else {
            userService.putState(chatId, State.ENTERING_NUMBER);
            return SendMessageFactory.enterNumberSendMessage(chatId);
        }
    }

    private SendMessage replyToDriverEnterNumber(long chatId, String number, Update upd) throws TelegramApiException {
        numberService.addNumber(chatId, number);
        return replyToChooseRoleDriver(chatId);
    }

    private SendMessage replyToEnterNumber(long chatId, String number, Update upd) throws TelegramApiException {
        numberService.addNumber(chatId, number);
        int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
        if (currentHour >= Constants.CURFEW_START_HOUR || currentHour < Constants.CURFEW_END_HOUR) {
            userService.putState(chatId, State.APPROVING_TRIP);
        } else {
            userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
        }
        return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), number, AbilityUtils.getUser(upd));
    }

    private SendMessage replyToEditAddress(long chatId, String message) throws TelegramApiException {
        tripService.setTripAddress(chatId, message);
        String currentDetails = tripService.getTripDetails(chatId);
        if (currentDetails == null) {
            userService.putState(chatId, State.EDITING_DETAILS);
            return SendMessageFactory.enterDetailsSendMessage(chatId);
        }
        userService.putState(chatId, State.EDITING_DETAILS);
        return SendMessageFactory.editDetailsSendMessage(chatId, tripService.getTripDetails(chatId));
    }

    public SendMessage replyToApproveTrip(long chatId, Update upd) throws TelegramApiException {
        int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
        if (currentHour < Constants.CURFEW_START_HOUR && currentHour >= Constants.CURFEW_END_HOUR) {
            userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
            return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                    tripService.getTripDetails(chatId), numberService.getNumber(chatId), AbilityUtils.getUser(upd));
        }

        userService.putState(chatId, State.LOOKING_FOR_DRIVER);
        tripService.addNewTripToQueue(chatId);
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

    public SendMessage replyToStopLookingForACar(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.TRIP_SEARCH_STOPPED);
        tripService.removeTripFromQueueByPassengerId(chatId);
        User user = userService.getUserInfo(chatId);
        return SendMessageFactory.tripSearchStoppedSendMessage(chatId, user,
                tripService.getTripAddress(chatId), tripService.getTripDetails(chatId),
                numberService.getNumber(chatId));
    }

    /**
     * Removes passenger trip application and sets state to creating new trip
     *
     * @param chatId chat id of the passenger
     * @return Message & menu to enter address
     */
    private SendMessage replyToEditTrip(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.EDITING_ADDRESS);
        tripService.removeTripFromQueueByPassengerId(chatId);
        return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
    }

    /**
     * Removes trip from passengers' trips queue
     *
     * @param chatId chat id of the passenger
     * @return Message & menu "Have a nice trip"
     */
    private SendMessage replyToFoundACar(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.FOUND_A_CAR);
        tripService.removeTripOnPassengerFoundACar(chatId);
        return SendMessageFactory.haveANiceTripSendMessage(chatId);
    }

    private String generateDriverOfferTripMessage(long chatId, QueueTrip queueTrip) {
        if (queueTrip == null) {
            return Constants.NO_TRIPS_MESSAGE;
        }

        User user = userService.getUserInfo(queueTrip.getPassengerChatId());
        return EscapeMessageService.escapeMessage(Constants.IS_LOOKING_FOR_CAR_MESSAGE,
                user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "",
                // todo: exception
                queueTrip.getAddress(), queueTrip.getDetails());
    }

    private SendMessage replyWithText(long chatId, String messageText) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .build();
    }

    private String currentScheduleMessage;
    private String currentScheduleTime;
//    private final Map<Long, Integer> currentScheduleMessageId = new HashMap<>();

//    public void sendNotificationToDrivers(long chatId, String currentScheduleMessage, String currentScheduleTime) throws TelegramApiException {
//        this.currentScheduleMessage = currentScheduleMessage;
//        this.currentScheduleTime = currentScheduleTime;
//        sendNotificationToDrivers(chatId, true);
//    }
//
//    private void sendNotificationToDrivers(long chatId) throws TelegramApiException {
//        sendNotificationToDrivers(chatId, false);
//    }

    private void sendNotificationToDrivers(long chatId, boolean mailing) throws TelegramApiException {
        if (currentScheduleMessage == null)
            return;

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .text(mailing ? currentScheduleMessage : currentScheduleTime)
                .chatId(String.valueOf(chatId));

        if (userService.getState(chatId) == State.DRIVER_ACTIVE) {
            if (tripService.getTripFromQueueByDriver(chatId) == null)
                builder.replyMarkup(ReplyMarkupFactory.noTripsReplyMarkup());
            else
                builder.replyMarkup(ReplyMarkupFactory.driverActiveReplyMarkup());
        } else return;

        sender.executeAsync(builder.build(), emptyCallback);
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
                        sendApprovingTripMessage(chatId));
        }
    }

    @SneakyThrows
    private void sendApprovingTripMessage(long chatId) {
        sender.executeAsync(SendMessageFactory.approvingTripSendMessage(chatId,
                tripService.getTripAddress(chatId), tripService.getTripDetails(chatId),
                numberService.getNumber(chatId), userService.getUserInfo(chatId)), emptyCallback);
    }
}
