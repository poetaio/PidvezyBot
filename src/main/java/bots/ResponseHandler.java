package bots;

import bots.factories.ReplyMarkupFactory;
import bots.factories.SendMessageFactory;
import bots.utils.Constants;
import bots.utils.EmptyCallback;
import models.utils.State;

import models.QueueTrip;

import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.*;
import services.driver_services.DriverService;
import services.passenger_services.PassengerService;
import services.trip_services.TripQueueService;
import services.trip_services.TripService;

import java.util.Calendar;

/**
 * Main class to handle all responses
 */
public class ResponseHandler {

    // TODO: REMOVE!!!
    private static ResponseHandler INSTANCE;

    public static ResponseHandler getInstance(MessageSender sender) {
        if (INSTANCE == null)
            INSTANCE = new ResponseHandler(sender);
        return INSTANCE;
    }

    private final MessageSender sender;

    private final UserService userService;
    private final PassengerService passengerService;
    private final DriverService driverService;
    private final TripService tripService;

    private final TripQueueService passengerQueueService;

    private final EmptyCallback emptyCallback;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;

        passengerService = new PassengerService();
        driverService = new DriverService();
        tripService = new TripService();

        userService = new UserService(driverService, tripService);

        passengerQueueService = TripQueueService.getInstance();
        emptyCallback = new EmptyCallback();

        setupDriverScheduler();
    }

    /**
     * Creates a thread to update all drivers' views of passengers trips
     */
    private void setupDriverScheduler() {
        new Thread(new DriverViewUpdateTask() {
            @Override
            protected void sendNoTripsAvailable(long chatId) throws TelegramApiException {
                sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE), emptyCallback);
            }

            @Override
            protected void sendTripOffer(long chatId, QueueTrip passengerDao) throws TelegramApiException {
                sender.executeAsync(SendMessageFactory.driverActiveSendMessage(chatId, generateDriverOfferTripMessage(chatId, passengerDao)), emptyCallback);
            }
        }).start();
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
            case DRIVER_TOOK_TRIP:
                messageToSend = onDriverTookTrip(chatId, message);
                break;
            case NO_TRIPS_AVAILABLE:
                messageToSend = onNoTripsAvailable(chatId, message);
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
            case FOUND_A_CAR:
                messageToSend = onFoundACar(chatId, message);
                break;
            default:
                messageToSend = SendMessage.builder().chatId(String.valueOf(chatId)).text(Constants.UNKNOWN_STATE_ERROR_MESSAGE).build();
        }

//        sender.executeAsync(messageToSend, emptyCallback);
        // to see errors in logs
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
                QueueTrip nextPassenger = passengerQueueService.getNextFree(chatId);
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(chatId, nextPassenger));
            case Constants.TAKE_TRIP:
                QueueTrip driverViewTrip = passengerQueueService.getPassengerDaoByDriver(chatId);
                if (driverViewTrip == null) {
                    // sending "that trip is already taken" (when it is being approved or taken fully)
                    sender.executeAsync(SendMessageFactory.tripAlreadyTakenSendMessage(chatId), emptyCallback);
                    // TODO: move to driver service, make one method getNextTrip(chatId)
                    // move logic to services as max as possible
                    driverService.resetDriverTime(chatId);
                    QueueTrip nextPassenger1 = passengerQueueService.getNextFree(chatId);
                    return SendMessageFactory.driverActiveSendMessage(chatId,
                            generateDriverOfferTripMessage(chatId, nextPassenger1));
                }
                driverService.unsubscribeDriverFromUpdate(chatId);
                userService.putState(chatId, State.DRIVER_TOOK_TRIP);

                User driver = userService.getUserInfo(chatId);

//                passengerQueueService.removeAndSaveInBufferByPassengerId(driverViewTrip.getPassengerChatId());
                tripService.takeDriverTrip(chatId);

                userService.putState(driverViewTrip.getPassengerChatId(), State.FOUND_A_CAR);
                sender.executeAsync(SendMessageFactory.noticingPassengerDriverTookTripSendMessage(driverViewTrip.getPassengerChatId(), driver),  emptyCallback);
                sender.executeAsync(SendMessageFactory.askingPassengerToInformAboutTripSendMessage(driverViewTrip.getPassengerChatId()),  emptyCallback);

                // TODO: NULLPOINTER CHECK
                return SendMessageFactory.driverTookTripSendMessage(chatId,
                        userService.getUserInfo(driverViewTrip.getPassengerChatId()),
                        driverViewTrip.getAddress(),
                        driverViewTrip.getDetails(),
                        passengerService.getNumber(driverViewTrip.getPassengerChatId()));
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(chatId, passengerQueueService.getPassengerDaoByDriver(chatId)));
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
                userService.putState(chatId, State.DRIVER_ACTIVE);
                sendNotificationToDrivers(chatId, false);
                tripService.dismissDriverTrip(chatId);
                driverService.subscribeDriverOnUpdate(chatId);
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(chatId, passengerQueueService.getNextFree(chatId)));
            default:
                QueueTrip driverPassenger = passengerQueueService.getPassengerDaoByDriver(chatId);
                // TODO: NULLPOINTER CHECK userInfo.get and passengerQueueService.getByDriver
                return SendMessageFactory.driverTookTripSendMessage(chatId, userService.getUserInfo(
                                passengerQueueService.getPassengerDaoByDriver(chatId).getPassengerChatId()),
                        driverPassenger.getAddress(),
                        driverPassenger.getDetails(),
                        passengerService.getNumber(driverPassenger.getPassengerChatId()));
        }
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
                return replyToApproveTrip(chatId);
            case Constants.CHANGE_TRIP_INFO:
                userService.putState(chatId, State.EDITING_ADDRESS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_DETAILS);
                return SendMessageFactory.editDetailsSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                        tripService.getTripDetails(chatId), passengerService.getNumber(chatId), upd);
        }
    }

    private SendMessage onTryAgainDuringCurfew(long chatId, String message, Update upd) throws TelegramApiException {
        int currentHour;
        switch (message) {
            case Constants.TRY_AGAIN:
                currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (currentHour < Constants.CURFEW_START_HOUR && currentHour > Constants.CURFEW_END_HOUR) {
                    return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                            tripService.getTripDetails(chatId), passengerService.getNumber(chatId), upd);
                }
                return replyToApproveTrip(chatId);
            case Constants.CHANGE_TRIP_INFO:
                userService.putState(chatId, State.EDITING_ADDRESS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripAddress(chatId));
            case Constants.BACK:
                userService.putState(chatId, State.EDITING_DETAILS);
                return SendMessageFactory.editAddressSendMessage(chatId, tripService.getTripDetails(chatId));
            default:
                currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (currentHour >= Constants.CURFEW_START_HOUR || currentHour <= Constants.CURFEW_END_HOUR) {
                    userService.putState(chatId, State.APPROVING_TRIP);
                }
                return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                        tripService.getTripDetails(chatId), passengerService.getNumber(chatId), upd);

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
                return replyToFoundACar(chatId);
            default:
                return replyWithText(chatId, Constants.REQUEST_PENDING_MESSAGE);
        }
//        if (message.equals(Constants.EDIT_TRIP)) {
//            // INACTIVE_TODO: track trip request recipients to delete it from every chat
////            return replyToCancelTrip(chatId);
//            return replyToEditTrip(chatId);
//        } else {
//
//        }
    }

    /**
     * Handles "Я знайшов транспорт" reply
     *
     * @param chatId chat id of the passenger
     * @return Choosing role text & menu
     * @throws TelegramApiException basic telegram exception
     */
    private SendMessage onFoundACar(long chatId, String message) throws TelegramApiException {
        switch (message){
            case Constants.FOUND_TRIP:
                tripService.takePassengerTrip(chatId);
//                passengerQueueService.removeTripFromBuffer(chatId);
                userService.putState(chatId, State.HAVE_A_NICE_TRIP);
                return SendMessageFactory.wishAGoodTripSendMessage(chatId);
            case Constants.FIND_AGAIN:
                tripService.dismissPassengerTrip(chatId);
//                passengerQueueService.returnTripFromBuffer(chatId);
                userService.putState(chatId, State.LOOKING_FOR_DRIVER);
                return SendMessageFactory.addressApprovedSendMessage(chatId);
            case Constants.THANKS:
                userService.putState(chatId, State.CHOOSING_ROLE);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.returnToSearchingSendMessage(chatId);
        }
    }

    // replyTo... - handlers to every message in every state
    public SendMessage replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.DRIVER_ACTIVE);
        // adding driver to both drivers list and update queue
        driverService.addDriver(chatId);
        sendNotificationToDrivers(chatId, false);
        QueueTrip tripInfo = passengerQueueService.getNextFree(chatId);
        if (tripInfo == null) {
            userService.putState(chatId, State.NO_TRIPS_AVAILABLE);
            return SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE);
        }

        userService.putState(chatId, State.DRIVER_ACTIVE);
        // todo: merge with generateDriverOfferTrip
        User passengerUserInfo = userService.getUserInfo(tripInfo.getPassengerChatId());
        String message = String.format(Constants.IS_LOOKING_FOR_CAR_MESSAGE,
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

    public SendMessage replyToStopBroadcast(long chatId) throws TelegramApiException {
//        userService.putState(chatId, State.DRIVER_INACTIVE);
        userService.putState(chatId, State.CHOOSING_ROLE);
        driverService.removeDriver(chatId);
//        SendMessageFactory.sendDriverInactiveMenu(chatId);
        return SendMessageFactory.chooseRoleSendMessage(chatId);
    }

    private SendMessage replyToEnterAddress(long chatId, String address) throws TelegramApiException {
        tripService.addAddressToTrip(chatId, address);
        userService.putState(chatId, State.ENTERING_DETAILS);
        return SendMessageFactory.enterDetailsSendMessage(chatId);
    }

    private SendMessage replyToEnterDetails(long chatId, String details, Update upd) throws TelegramApiException {
        tripService.addDetailsToTrip(chatId, details);
        String number = passengerService.getNumber(chatId);
        if (number != null) {
            userService.putState(chatId, State.APPROVING_TRIP);
            return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                    tripService.getTripDetails(chatId), number, upd);
        } else {
            userService.putState(chatId, State.ENTERING_NUMBER);
            return SendMessageFactory.enterNumberSendMessage(chatId);
        }
    }

    private SendMessage replyToEnterNumber(long chatId, String number, Update upd) throws TelegramApiException {
        passengerService.addNumber(chatId, number);
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (currentHour >= Constants.CURFEW_START_HOUR || currentHour <= Constants.CURFEW_END_HOUR) {
            userService.putState(chatId, State.APPROVING_TRIP);
        } else {
            userService.putState(chatId, State.TRY_AGAIN_DURING_CURFEW);
        }
        return SendMessageFactory.approvingTripSendMessage(chatId, tripService.getTripAddress(chatId),
                tripService.getTripDetails(chatId), number, upd);
    }

    private SendMessage replyToEditAddress(long chatId, String message) throws TelegramApiException {
        tripService.addAddressToTrip(chatId, message);
        String currentDetails = tripService.getTripDetails(chatId);
        if (currentDetails == null) {
            userService.putState(chatId, State.EDITING_DETAILS);
            return SendMessageFactory.enterDetailsSendMessage(chatId);
        }
        userService.putState(chatId, State.EDITING_DETAILS);
        return SendMessageFactory.editDetailsSendMessage(chatId, tripService.getTripDetails(chatId));
    }

    public SendMessage replyToApproveTrip(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.LOOKING_FOR_DRIVER);
        tripService.addNewTrip(chatId);
//        tripService.approveTrip(chatId);
        return SendMessageFactory.addressApprovedSendMessage(chatId);
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
        // todo: handle if driver views trip
        userService.putState(chatId, State.FOUND_A_CAR);
        tripService.removeTripFromQueueByPassengerId(chatId);
        tripService.passengerFoundACar(chatId);
        return SendMessageFactory.haveANiceTripSendMessage(chatId);
    }

    private String generateDriverOfferTripMessage(long chatId, QueueTrip queuePassengerDao) {
        if (queuePassengerDao == null) {
            return Constants.NO_TRIPS_MESSAGE;
        }

        User user = userService.getUserInfo(queuePassengerDao.getPassengerChatId());
        return String.format(Constants.IS_LOOKING_FOR_CAR_MESSAGE,
                user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "",
                // todo: exception
                queuePassengerDao.getAddress(), queuePassengerDao.getDetails());
    }

//    private String generateDriverTookTripMessage(long chatId, Trip queuePassengerDao) {
//        return String.format("%s шукає транспорт з вокзалу на %s \n\n%s",
//                userService.getUserInfo(chatId).getFirstName(), queuePassengerDao.getAddress(),
//                queuePassengerDao.getDetails());
//    }

//    private void onToCancelTrip(long chatId, User user, String address) throws TelegramApiException {
//    private SendMessage replyToCancelTrip(long chatId) throws TelegramApiException {
//        userService.putState(chatId, State.ENTERING_ADDRESS);
//      }

//    private void deleteLastBotMessage(long chatId) throws TelegramApiException {
//        Integer botMessageToUpdate = updateMessageService.getBotMessageToUpdate(chatId);
//        if (botMessageToUpdate != null) {
//            DeleteMessage deleteLastBotMessage = DeleteMessage.builder()
//                    .messageId(botMessageToUpdate)
//                    .chatId(String.valueOf(chatId))
//                    .build();
//            try {
//                sender.execute(deleteLastBotMessage);
//            } catch (Exception ignored) {}
//        }
//    }

    private SendMessage replyWithText(long chatId, String messageText) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .build();
    }

    private String currentScheduleMessage;
    private String currentScheduleTime;
//    private final Map<Long, Integer> currentScheduleMessageId = new HashMap<>();

    public void sendNotificationToDrivers(long chatId, String currentScheduleMessage, String currentScheduleTime) throws TelegramApiException {
        this.currentScheduleMessage = currentScheduleMessage;
        this.currentScheduleTime = currentScheduleTime;
        sendNotificationToDrivers(chatId, true);
    }

    private void sendNotificationToDrivers(long chatId) throws TelegramApiException {
        sendNotificationToDrivers(chatId, false);
    }

    private void sendNotificationToDrivers(long chatId, boolean mailing) throws TelegramApiException {
        if (currentScheduleMessage == null)
            return;

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .text(mailing ? currentScheduleMessage : currentScheduleTime)
                .chatId(String.valueOf(chatId));

        if (userService.getState(chatId) == State.DRIVER_ACTIVE) {
            if (passengerQueueService.getPassengerDaoByDriver(chatId) == null)
                builder.replyMarkup(ReplyMarkupFactory.noTripsReplyMarkup());
            else
                builder.replyMarkup(ReplyMarkupFactory.driverActiveReplyMarkup());
        }
        else if (userService.getState(chatId) == State.DRIVER_TOOK_TRIP)
            builder.replyMarkup(ReplyMarkupFactory.driverTookTripReplyKeyboard());
        else return;

        sender.executeAsync(builder.build(), emptyCallback);
    }

//    private void removeScheduleMessage(long chatId) {
//        try {
//            if (currentScheduleMessageId.get(chatId) != null)
//                sender.execute(DeleteMessage.builder()
//                        .chatId(String.valueOf(chatId))
//                        .messageId(currentScheduleMessageId.get(chatId))
//                        .build());
//        } catch (Exception ignored) {}
//    }

    public String getCurrentScheduleMessage() {
        return currentScheduleMessage;
    }
}
