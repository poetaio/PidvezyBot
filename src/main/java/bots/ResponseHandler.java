package bots;

import bots.factories.ReplyMarkupFactory;
import bots.factories.SendMessageFactory;
import bots.utils.Constants;
import models.utils.State;

import models.Trip;

import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;
import services.*;
import services.driver_services.DriverService;
import services.passenger_services.PassengerQueueService;
import services.passenger_services.PassengerService;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

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

    private final PassengerQueueService passengerQueueService;
    private final UpdateMessageService updateMessageService;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;

        userService = UserService.getInstance();
        passengerService = PassengerService.getInstance();
        driverService = DriverService.getInstance();

        passengerQueueService = PassengerQueueService.getInstance();
        updateMessageService = UpdateMessageService.getInstance();

        setupDriverScheduler();
    }

    /**
     * Creates a thread to update all drivers' views of passengers trips
     */
    private void setupDriverScheduler() {
        new Thread(new DriverViewUpdateTask() {
            @Override
            protected void sendNoTripsAvailable(long chatId) throws TelegramApiException {
                // todo: replace with async
                synchronized (SendingMessageService.MessageSending) {
                    Message sentMessage = sender.execute(SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE));
                    deleteLastBotMessage(chatId);
                    updateMessageService.putBotMessageToUpdate(chatId, sentMessage.getMessageId());
                }
            }
            @Override
            protected void sendTripOffer(long chatId, Trip passengerDao) throws TelegramApiException {
                // todo: replace with async
                synchronized (SendingMessageService.MessageSending) {
                    Message sentMessage = sender.execute(SendMessageFactory.driverActiveSendMessage(chatId, generateDriverOfferTripMessage(passengerDao)));
                    deleteLastBotMessage(chatId);
                    updateMessageService.putBotMessageToUpdate(chatId, sentMessage.getMessageId());
                }
            }
        }).start();
    }

    /**
     * Replies to /start command with two roles to choose from
     * @param chatId of the chat message comes from
     * @param upd Update entity containing info
     */
    public void replyToStart(long chatId, Update upd) throws TelegramApiException {
        userService.performCleanup(chatId);
        // deleting previous user message
        Integer userMessageToDelete = updateMessageService.getUserMessageToUpdate(chatId);
        if (userMessageToDelete != null) {
            DeleteMessage deleteLastUserMessage = DeleteMessage.builder()
                    .messageId(userMessageToDelete)
                    .chatId(String.valueOf(chatId))
                    .build();
            try {
                sender.execute(deleteLastUserMessage);
            } catch (Exception ignored) {}
        }
        updateMessageService.putUserMessageToUpdate(chatId, upd.getMessage().getMessageId());
        removeScheduleMessage(chatId);
        userService.putState(chatId, State.CHOOSING_ROLE);
        // todo: remove
        synchronized (SendingMessageService.MessageSending) {
            deleteLastBotMessage(chatId);
            Message messageSent = sender.execute(SendMessageFactory.chooseRoleSendMessage(chatId));
            updateMessageService.putUserMessageToUpdate(chatId, upd.getMessage().getMessageId());
            updateMessageService.putBotMessageToUpdate(chatId, messageSent.getMessageId());
        }
    }

    /**
     * Main handler, handles menu actions & states
     * @param upd takes Update
     * @throws TelegramApiException Classic telegram exception
     */
    public void handleUpdate(Update upd) throws TelegramApiException {
        long chatId = AbilityUtils.getChatId(upd);

        if (!upd.hasMessage() || !upd.getMessage().hasText()) {
            SendMessage.builder().chatId(String.valueOf(chatId)).text("Пусте повідомлення :(");
            return;
        }

        if (upd.getMessage().getText().indexOf('/') == 0)
            return;

        // deleting previous user message
        Integer userMessageToDelete = updateMessageService.getUserMessageToUpdate(chatId);
        if (userMessageToDelete != null) {
            DeleteMessage deleteLastUserMessage = DeleteMessage.builder()
                    .messageId(userMessageToDelete)
                    .chatId(String.valueOf(chatId))
                    .build();
            try {
                sender.execute(deleteLastUserMessage);
            } catch (Exception ignored) {}
        }
        updateMessageService.putUserMessageToUpdate(chatId, upd.getMessage().getMessageId());

        String message = upd.getMessage().getText();
        SendMessage messageToSend;

        State currentState = userService.getState(chatId);
        if (currentState == null) {
            replyToStart(chatId, upd);
            return;
        }

        switch (currentState) {
            case CHOOSING_ROLE:
                userService.putUserInfo(chatId, AbilityUtils.getUser(upd));
//                userInfo.putIfAbsent(chatId, AbilityUtils.getUser(upd));
                messageToSend = onChoosingRole(chatId, message);
                break;

            // Driver states
            case DRIVER_ACTIVE:
                messageToSend = onDriverActive(chatId, message);
                break;
            case DRIVER_TOOK_TRIP:
                messageToSend = onDriverTookTrip(chatId, message);
//            case DRIVER_INACTIVE:
//                onDriverInactive(chatId, message);
//                break;
                break;

            // Passenger states
            case ENTERING_ADDRESS:
                messageToSend = onEnteringAddress(chatId, message);
                break;
            case ENTERING_DETAILS:
                messageToSend = onEnteringDetails(chatId, message);
                break;
            case ENTERING_ON_STATION:
                messageToSend = onEnteringOnStation(chatId, message, upd);
                break;
            case CHECKING_OUT_ON_STATION:
                messageToSend = onCheckingOutOnStation(chatId, message, upd);
                break;
            case APPROVING_TRIP:
                messageToSend = onApprovingTrip(chatId, message, upd);
                break;
            case LOOKING_FOR_DRIVER:
                messageToSend = onLookingForDriver(chatId, message);
                break;
            case FOUND_A_CAR:
                messageToSend = onFoundACar(chatId);
                break;
            default:
                messageToSend = SendMessage.builder().chatId(String.valueOf(chatId)).text(Constants.UNKNOWN_STATE_ERROR_MESSAGE).build();
        }

        synchronized (SendingMessageService.MessageSending) {
            // deleting previous bot message
            Message messageSent = sender.execute(messageToSend);
            deleteLastBotMessage(chatId);
            updateMessageService.putBotMessageToUpdate(chatId, messageSent.getMessageId());
        }
    }

    /**
     * Choosing role handler
     * @param chatId user chat id
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
     * @param chatId user chat Id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     * @return Message to send to user
     */
    private SendMessage onDriverActive(long chatId, String message) throws TelegramApiException {
        switch (message) {
//            case Constants.STOP_BROADCAST:
//                replyToStopBroadcast(chatId);
//                break;
            case Constants.NEXT_TRIP:
                // if current driver's view is "No trips available" button NEXT_TRIP is unavailable as well
                Trip driverLastPassenger = passengerQueueService.getPassengerDaoByDriver(chatId);
                if (driverLastPassenger == null) {
                    // TODO: move logic to separate method (show no trips available or next trip)
                    return SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE);
                }
                driverService.resetDriverTime(chatId);
                Trip nextPassenger = passengerQueueService.getNextFree(chatId);
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(nextPassenger));
            case Constants.TAKE_TRIP:
                // if current driver's view is "No trips available" button TAKE_TRIP is unavailable as well
                Trip driverPassenger = passengerQueueService.getPassengerDaoByDriver(chatId);
                if (driverPassenger == null) {
                    return SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE);
                }
                // we just give a driver passenger's contact info,
                // and wait till passenger hits "I found a car" button
                // then it is eventually removed from passenger's queue
                driverService.unsubscribeDriverFromUpdate(chatId);
                userService.putState(chatId, State.DRIVER_TOOK_TRIP);
                // TODO: NULLPOINTER CHECK
                return SendMessageFactory.driverTookTripSendMessage(chatId,
                        userService.getUserInfo(driverPassenger.getPassengerChatId()));
            case Constants.BACK:
                userService.putState(chatId, State.CHOOSING_ROLE);
                removeScheduleMessage(chatId);
                driverService.removeDriver(chatId);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                // TODO: move logic to separate method (show no trips available or next trip)
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(passengerQueueService.getPassengerDaoByDriver(chatId)));
        }
    }

    /**
     * Handles State after driver takes trip, hits "Відгукнутися"
     * @param chatId driver chat id
     * @param message message sent by driver
     * @return message to reply with
     * @throws TelegramApiException
     */
    private SendMessage onDriverTookTrip(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                userService.putState(chatId, State.DRIVER_ACTIVE);
                sendNotificationToDrivers(chatId, false);
                driverService.subscribeDriverOnUpdate(chatId);
                return SendMessageFactory.driverActiveSendMessage(chatId,
                        generateDriverOfferTripMessage(passengerQueueService.getNextFree(chatId)));
            default:
                // TODO: NULLPOINTER CHECK userInfo.get and passengerQueueService.getByDriver
                return SendMessageFactory.driverTookTripSendMessage(chatId, userService.getUserInfo(
                        passengerQueueService.getPassengerDaoByDriver(chatId).getPassengerChatId()));
        }
    }

    /**
     * Unsubscribe from getting trip applications
     * @param chatId user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onDriverInactive(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.RESUME_BROADCAST:
                return replyToChooseRoleDriver(chatId);
            case Constants.BACK:
                // TODO: refactor move to separate method
                userService.putState(chatId, State.CHOOSING_ROLE);
                return SendMessageFactory.chooseRoleSendMessage(chatId);
            default:
                return SendMessageFactory.driverInactiveSendMessage(chatId);
        }
    }

    /**
     * Entering passenger destination address handler,
     * if entered passenger address is valid saves it for future trip
     * @param chatId user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringAddress(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            // TODO: refactor move to separate method
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
     * @param chatId user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringDetails(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            // TODO: refactor move to separate method
            userService.putState(chatId, State.ENTERING_ADDRESS);
            return SendMessageFactory.enterAddressSendMessage(chatId);
        } else if (!message.isEmpty() && !message.isBlank()) {
            return replyToEnterDetails(chatId, message);
        } else {
            return SendMessageFactory.enterDetailsSendMessage(chatId);
        }
    }

    /**
     * Handles entering if passenger is on station (y/n)
     * If "yes" is entered passenger is asked for trip info approving
     * @param chatId user chat id
     * @param message message sent by user
     * @param upd update entity
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringOnStation(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.ON_STATION_NO:
                return replyToNotOnStation(chatId);
            case Constants.ON_STATION_YES:
                return replyToOnStation(chatId, upd);
            case Constants.BACK:
                userService.putState(chatId, State.ENTERING_DETAILS);
                return SendMessageFactory.enterDetailsSendMessage(chatId);
            default:
                return SendMessageFactory.enterOnStationSendMessage(chatId);
        }
    }

    /**
     * Handles passenger's checking out on station after choosing no in "Are you on station" menu
     * @param chatId user chat id
     * @param message message sent by user
     * @param upd update entity
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onCheckingOutOnStation(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.I_AM_ON_STATION:
                return replyToOnStation(chatId, upd);
            case Constants.BACK:
                userService.putState(chatId, State.ENTERING_ON_STATION);
                return SendMessageFactory.enterOnStationSendMessage(chatId);
            default:
                return SendMessageFactory.checkingOutOnStationSendMessage(chatId);
        }
    }

    /**
     * Handles state when passenger is approving trip
     * @param chatId passenger chat id
     * @param message message sent by passenger
     * @param upd Update entity containing info
     * @return message to reply with
     * @throws TelegramApiException basic exc
     */
    private SendMessage onApprovingTrip(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.APPROVE_TRIP:
                return replyToApproveAddress(chatId);
            case Constants.CHANGE_TRIP_INFO:
                // TODO: save or delete current address and details
                userService.putState(chatId, State.ENTERING_ADDRESS);
                return SendMessageFactory.enterAddressSendMessage(chatId);
            case Constants.BACK:
                // TODO: refactor move to separate method
                userService.putState(chatId, State.ENTERING_DETAILS);
                return SendMessageFactory.enterDetailsSendMessage(chatId);
            default:
                // TODO: refactor move to separate method
                return SendMessageFactory.approvingTripSendMessage(chatId, passengerService.getAddress(chatId),
                        passengerService.getDetails(chatId), upd);
        }
    }

    /**
     * Handles state when passenger has approved trip and is looking for driver
     * @param chatId passenger chat id
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
     * @param chatId chat id of the passenger
     * @return Choosing role text & menu
     * @throws TelegramApiException basic telegram exception
     */
    private SendMessage onFoundACar(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.CHOOSING_ROLE);
        return SendMessageFactory.chooseRoleSendMessage(chatId);
    }

    // replyTo... - handlers to every message in every state
    public SendMessage replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.DRIVER_ACTIVE);
        // adding driver to both drivers list and update queue
        driverService.addDriver(chatId);
        sendNotificationToDrivers(chatId, false);
        Trip tripInfo = passengerQueueService.getNextFree(chatId);
        if (tripInfo == null) {
            return SendMessageFactory.driverActiveSendMessage(chatId, Constants.NO_TRIPS_MESSAGE);
        }

        // todo: merge with generateDriverOfferTrip
        User passengerUserInfo = userService.getUserInfo(tripInfo.getPassengerChatId());
        String message = String.format("%s%s шукає транспорт з вокзалу на %s \n\n%s",
                passengerUserInfo.getFirstName(), passengerUserInfo.getLastName() != null ? " " + passengerUserInfo.getLastName() : "",
                tripInfo.getAddress(), tripInfo.getDetails());
        return SendMessageFactory.driverActiveSendMessage(chatId, message);
    }

    public SendMessage replyToChooseRolePassenger(long chatId) throws TelegramApiException {
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
        passengerService.addAddress(chatId, address);
        userService.putState(chatId, State.ENTERING_DETAILS);
        return SendMessageFactory.enterDetailsSendMessage(chatId);
    }

    private SendMessage replyToEnterDetails(long chatId, String details) throws TelegramApiException {
        passengerService.addDetails(chatId, details);
        // TODO: username or phone may be absent
        // TODO: "please allow your phone info, or enter your phone"
        userService.putState(chatId, State.ENTERING_ON_STATION);
        return SendMessageFactory.enterOnStationSendMessage(chatId);
    }

    private SendMessage replyToNotOnStation(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.CHECKING_OUT_ON_STATION);
        return SendMessageFactory.checkingOutOnStationSendMessage(chatId);
    }

    private SendMessage replyToOnStation(long chatId, Update upd) throws TelegramApiException {
        userService.putState(chatId, State.APPROVING_TRIP);
        return SendMessageFactory.approvingTripSendMessage(chatId, passengerService.getAddress(chatId),
                passengerService.getDetails(chatId), upd);
    }

    public SendMessage replyToApproveAddress(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.LOOKING_FOR_DRIVER);
        passengerQueueService.add(new Trip(chatId, passengerService.getAddress(chatId),
                passengerService.getDetails(chatId)));
        return SendMessageFactory.addressApprovedSendMessage(chatId);
    }

    /**
     * Removes passenger trip application and sets state to creating new trip
     * @param chatId chat id of the passenger
     * @return Message & menu to enter address
     */
    private SendMessage replyToEditTrip(long chatId) throws TelegramApiException {
        userService.putState(chatId, State.ENTERING_ADDRESS);
        // todo: add status inactive instead of removing trip totally
        // todo: move to storage with driver id, who's taken the trip
        passengerQueueService.removeByPassengerId(chatId);
        return SendMessageFactory.enterAddressSendMessage(chatId);
    }

    /**
     * Removes trip from passengers' trips queue
     * @param chatId chat id of the passenger
     * @return Message & menu "Have a nice trip"
     */
    private SendMessage replyToFoundACar(long chatId) throws TelegramApiException {
        // todo: handle if driver views trip
        userService.putState(chatId, State.FOUND_A_CAR);
        passengerQueueService.removeByPassengerId(chatId);
        return SendMessageFactory.haveANiceTripSendMessage(chatId);
    }

    private String generateDriverOfferTripMessage(Trip queuePassengerDao) {
        if (queuePassengerDao == null)
            return Constants.NO_TRIPS_MESSAGE;

        User user = userService.getUserInfo(queuePassengerDao.getPassengerChatId());
        return String.format("%s%s шукає транспорт з вокзалу на %s \n\n%s\n\n" + "(Заявка оновлюється кожні " + Constants.DRIVER_UPDATE_INTERVAL + " секунд)",
                user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "",
                // todo: exception
                queuePassengerDao.getAddress(), queuePassengerDao.getDetails());
    }

//    private String generateDriverTookTripMessage(long chatId, Trip queuePassengerDao) {
//        // TODO: NULLPOINTER CHECK userInfo.get
//        return String.format("%s шукає транспорт з вокзалу на %s \n\n%s",
//                userService.getUserInfo(chatId).getFirstName(), queuePassengerDao.getAddress(),
//                queuePassengerDao.getDetails());
//    }

//    private void onToCancelTrip(long chatId, User user, String address) throws TelegramApiException {
//    private SendMessage replyToCancelTrip(long chatId) throws TelegramApiException {
//        userService.putState(chatId, State.ENTERING_ADDRESS);
//
//        return SendMessageFactory.makeEnterAddressMenu(chatId);
//        DeleteMessage deleteMessage = new DeleteMessage();
//
//        for (Long driver : driversList) {
//            deleteMessage.setChatId(String.valueOf(driver));
//            deleteMessage.setMessageId(1);
//
//            se.exe(deleteMessage);
//        }
//        replyWithText(chatId, Constants.TRIP_CANCELED_SUCCESS_MESSAGE);
//    }

    // todo: race condition two threads try to delete one message
    private void deleteLastBotMessage(long chatId) throws TelegramApiException {
        Integer botMessageToUpdate = updateMessageService.getBotMessageToUpdate(chatId);
        if (botMessageToUpdate != null) {
            DeleteMessage deleteLastBotMessage = DeleteMessage.builder()
                    .messageId(botMessageToUpdate)
                    .chatId(String.valueOf(chatId))
                    .build();
            try {
                sender.execute(deleteLastBotMessage);
            } catch (Exception ignored) {}
        }
    }

    private SendMessage replyWithText(long chatId, String messageText) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .build();
    }

//    // TODO: remove
//    public void sendBroadcastToDrivers(String message) throws TelegramApiException {
//        for (DriverUpdateDao driver : DriverUpdateService.getInstance().getAll()) {
//            System.out.println(driver.getChatId());
//            SendMessage.SendMessageBuilder builder = SendMessage.builder()
//                    .text(message)
//                    .chatId(String.valueOf(driver.getChatId()));
//
//            State driverState = userService.getState(driver.getChatId());
//
//            if (driverState == State.DRIVER_ACTIVE)
//                builder.replyMarkup(KeyboardFactory.driverActiveReplyMarkup());
//            else if (driverState == State.DRIVER_TOOK_TRIP)
//                builder.replyMarkup(KeyboardFactory.driverTookTripReplyKeyboard());
//            else return;
//            se.exe(builder.build());
//        }
//    }

    private String currentScheduleMessage;
    private String currentScheduleTime;
    private final Map<Long, Integer> currentScheduleMessageId = new HashMap<>();

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

        sender.executeAsync(builder.build(), new SentCallback<Message>() {
            @SneakyThrows
            @Override
            public void onResult(BotApiMethod<Message> botApiMethod, Message message) {
                removeScheduleMessage(chatId);
                currentScheduleMessageId.put(chatId, message.getMessageId());
            }

            @Override
            public void onError(BotApiMethod<Message> botApiMethod, TelegramApiRequestException e) {}
            @Override
            public void onException(BotApiMethod<Message> botApiMethod, Exception e) {}
        });
    }

    private void removeScheduleMessage(long chatId) {
        try {
            if (currentScheduleMessageId.get(chatId) != null)
                sender.execute(DeleteMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(currentScheduleMessageId.get(chatId))
                        .build());
        } catch (Exception ignored) {}
    }

    public String getCurrentScheduleMessage() {
        return currentScheduleMessage;
    }
}
