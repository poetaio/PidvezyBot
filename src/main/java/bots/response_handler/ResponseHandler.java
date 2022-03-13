package bots.response_handler;

import bots.DriverMessageScheduler;
import bots.MenuSender;
import bots.utils.Constants;
import bots.utils.Role;
import bots.utils.State;
import models.dao.QueuePassengerDao;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.DriverService;
import services.PassengerQueueService;
import services.TestDataService;
import services.UpdateMessageService;

import java.util.HashMap;
import java.util.Map;

public class ResponseHandler {
    private final MessageSender sender;
    private final MenuSender menuSender;

    private final Map<Long, State> chatStates;

    // TODO: REFACTOR ASAP!!!!!!!!!!!
    // passenger maps
    private final Map<Long, User> userInfo;
    private final Map<Long, String> userAddress;
    private final Map<Long, String> userDetails;

    // driver maps
    private final DriverService driverService = DriverService.getInstance();
    private final PassengerQueueService passengerQueueService;
    private final UpdateMessageService updateMessageService;

    // TODO: divide handler
//    private final Map<Long, Role> userRole;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;
        menuSender = new MenuSender(sender);

        chatStates = new HashMap<>();
        userAddress = new HashMap<>();
        userDetails = new HashMap<>();

        // todo: add if on test mode
        userInfo = TestDataService.getTestUserInfo();

        passengerQueueService = PassengerQueueService.getInstance();
        updateMessageService = UpdateMessageService.getInstance();

        // TODO: divide handler
//        userRole = new HashMap<>();
        setupDriverScheduler();
    }

    private void setupDriverScheduler() {
        new Thread(new DriverMessageScheduler() {
            @Override
            protected void sendNoTripsAvailable(long chatId) throws TelegramApiException {
                deleteLastBotMessage(chatId);
                Message sentMessage = sender.execute(menuSender.sendDriverActiveMenu(chatId, Constants.NO_TRIPS_MESSAGE));
                updateMessageService.putBotMessageToUpdate(chatId, sentMessage.getMessageId());
            }
            @Override
            protected void sendTripOffer(long chatId, QueuePassengerDao passengerDao) throws TelegramApiException {
                deleteLastBotMessage(chatId);
                Message sentMessage = sender.execute(menuSender.sendDriverActiveMenu(chatId, generateDriverOfferTripMessage(passengerDao)));
                updateMessageService.putBotMessageToUpdate(chatId, sentMessage.getMessageId());
            }
        }).start();
    }

    /**
     * Replies to /start command with two roles to choose from
     * @param chatId of the chat message comes from
     */
    public void replyToStart(long chatId, Update upd) throws TelegramApiException{
        chatStates.put(chatId, State.CHOOSING_ROLE);
        sender.execute(menuSender.sendChooseRoleMenu(chatId));
        updateMessageService.putUserMessageToUpdate(chatId, upd.getMessage().getMessageId());
    }

    /**
     * Main handler, handles menu actions & states
     * @param upd takes Update
     * @throws TelegramApiException Classic telegram exception
     */
    public void handleUpdate(Update upd) throws TelegramApiException {
        long chatId = AbilityUtils.getChatId(upd);

        if (!upd.hasMessage() || !upd.getMessage().hasText() || upd.getMessage().getText().indexOf('/') == 0) {
            SendMessage.builder().chatId(String.valueOf(chatId)).text("Пусте повідомлення :(");
            return;
        }

        // deleting previous user message
        Integer userMessageToDelete = updateMessageService.getUserMessageToUpdate(chatId);
        if (userMessageToDelete != null) {
            DeleteMessage deleteLastUserMessage = DeleteMessage.builder()
                    .messageId(userMessageToDelete)
                    .chatId(String.valueOf(chatId))
                    .build();
            sender.execute(deleteLastUserMessage);
        }
        updateMessageService.putUserMessageToUpdate(chatId, upd.getMessage().getMessageId());

        String message = upd.getMessage().getText();
        SendMessage messageToSend;

        switch (chatStates.get(chatId)) {
            case CHOOSING_ROLE:
                userInfo.put(chatId, AbilityUtils.getUser(upd));
//                userInfo.putIfAbsent(chatId, AbilityUtils.getUser(upd));
                messageToSend = onChoosingRole(chatId, message);
                break;

            // Driver states
            case DRIVER_ACTIVE:
                messageToSend = onDriverActive(chatId, message, upd);
                break;
            case DRIVER_TOOK_TRIP:
                messageToSend = onDriverTookTrip(chatId, message, upd);
//            case DRIVER_INACTIVE:
//                onDriverInactive(chatId, message);
//                break;

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
            default:
                messageToSend = SendMessage.builder().chatId(String.valueOf(chatId)).text(Constants.UNKNOWN_STATE_ERROR_MESSAGE).build();
        }

        // deleting previous bot message
        deleteLastBotMessage(chatId);
        Message messageSent = sender.execute(messageToSend);
        updateMessageService.putBotMessageToUpdate(chatId, messageSent.getMessageId());
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
                return menuSender.sendChooseRoleMenu(chatId);
        }
    }

    /**
     * Driver role choosing handler, subscribe to getting trip applications
     * @param chatId user chat Id
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onDriverActive(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
//            case Constants.STOP_BROADCAST:
//                replyToStopBroadcast(chatId);
//                break;
            case Constants.NEXT_TRIP:
                QueuePassengerDao driverLastPassenger = passengerQueueService.getPassengerDaoByDriver(chatId);
                if (driverLastPassenger == null) {
                    // TODO: move logic to separate method (show no trips available or next trip)
                    return menuSender.sendDriverActiveMenu(chatId, Constants.NO_TRIPS_MESSAGE);
                }
                driverService.resetDriverTime(chatId);
                QueuePassengerDao nextPassenger = passengerQueueService.getNextFree(chatId);
                return menuSender.sendDriverActiveMenu(chatId,
                        generateDriverOfferTripMessage(nextPassenger));
            case Constants.TAKE_TRIP:
                QueuePassengerDao driverPassenger = passengerQueueService.getPassengerDaoByDriver(chatId);
                if (driverPassenger == null) {
                    // TODO: move logic to separate method (show no trips available or next trip)
                    return menuSender.sendDriverActiveMenu(chatId, Constants.NO_TRIPS_MESSAGE);
                }
                // TODO: implement removing from queue logic, canceling taking, etc...
                passengerQueueService.remove(chatId);
                chatStates.put(chatId, State.DRIVER_TOOK_TRIP);
                return menuSender.sendDriverTookTripMenu(chatId,
                        userInfo.get(driverPassenger.getPassengerChatId()));
            case Constants.BACK:
                chatStates.put(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                return menuSender.sendChooseRoleMenu(chatId);
            default:
                // TODO: move logic to separate method (show no trips available or next trip)
                return menuSender.sendDriverActiveMenu(chatId,
                        generateDriverOfferTripMessage(passengerQueueService.getPassengerDaoByDriver(chatId)));
        }
    }

    private SendMessage onDriverTookTrip(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                return replyWithText(chatId, "Not implemented");
            default:
                return menuSender.sendDriverTookTripMenu(chatId, userInfo.get(
                        passengerQueueService.getPassengerDaoByDriver(chatId).getPassengerChatId()));
        }
    }

    /**
     * Unsubscribe from getting trip applications
     * @param chatId
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onDriverInactive(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.RESUME_BROADCAST:
                return replyToChooseRoleDriver(chatId);
            case Constants.BACK:
                // TODO: refactor move to separate method
                chatStates.put(chatId, State.CHOOSING_ROLE);
                return menuSender.sendChooseRoleMenu(chatId);
            default:
                return menuSender.sendDriverInactiveMenu(chatId);
        }
    }

    /**
     * Entering passenger destination address handler,
     * if entered passenger address is valid saves it for future trip
     * @param chatId
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringAddress(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            // TODO: refactor move to separate method
            chatStates.put(chatId, State.CHOOSING_ROLE);
            return menuSender.sendChooseRoleMenu(chatId);
        } else if (!message.isEmpty() && !message.isBlank()) {
            return replyToEnterAddress(chatId, message);
        } else {
            return menuSender.sendEnterAddressMenu(chatId);
        }
    }

    /**
     * Entering passenger destination address handler,
     * if entered passenger details are valid saves them for future trip
     * @param chatId
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringDetails(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            // TODO: refactor move to separate method
            chatStates.put(chatId, State.ENTERING_ADDRESS);
            return menuSender.sendEnterAddressMenu(chatId);
        } else if (!message.isEmpty() && !message.isBlank()) {
            return replyToEnterDetails(chatId, message);
        } else {
            return menuSender.sendEnterDetailsMenu(chatId);
        }
    }

    /**
     * Handles entering if passenger is on station (y/n)
     * If "yes" is entered passenger is asked for trip info approving
     * @param chatId
     * @param message
     * @param upd
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onEnteringOnStation(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.ON_STATION_NO:
                return replyToNotOnStation(chatId);
            case Constants.ON_STATION_YES:
                return replyToOnStation(chatId, upd);
            case Constants.BACK:
                chatStates.put(chatId, State.ENTERING_DETAILS);
                return menuSender.sendEnterDetailsMenu(chatId);
            default:
                return menuSender.sendEnterOnStationMenu(chatId);
        }
    }

    /**
     * Handles passenger's checking out on station after choosing no in "Are you on station" menu
     * @param chatId
     * @param message
     * @param upd
     * @throws TelegramApiException Classic telegram exception
     */
    private SendMessage onCheckingOutOnStation(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.I_AM_ON_STATION:
                return replyToOnStation(chatId, upd);
            case Constants.BACK:
                chatStates.put(chatId, State.ENTERING_ON_STATION);
                return menuSender.sendEnterOnStationMenu(chatId);
            default:
                return menuSender.sendCheckingOutOnStationMenu(chatId);
        }
    }

    private SendMessage onApprovingTrip(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.APPROVE_TRIP:
                return replyToApproveAddress(chatId);
            case Constants.CHANGE_TRIP_INFO:
                // TODO: save or delete current address and details
                chatStates.put(chatId, State.ENTERING_ADDRESS);
                return menuSender.sendEnterAddressMenu(chatId);
            case Constants.BACK:
                // TODO: refactor move to separate method
                chatStates.put(chatId, State.ENTERING_DETAILS);
                return menuSender.sendEnterDetailsMenu(chatId);
            default:
                // TODO: refactor move to separate method
                return menuSender.sendApprovingTripMenu(chatId, userAddress.get(chatId), userDetails.get(chatId), upd);
        }
    }

    private SendMessage onLookingForDriver(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.CANCEL_TRIP)) {
            // TODO: track trip request recipients to delete it from every chat
            return replyToCancelTrip(chatId);
        } else {
            return replyWithText(chatId, Constants.REQUEST_PENDING_MESSAGE);
        }
    }

    public SendMessage replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.DRIVER_ACTIVE);
        // adding driver to both drivers list and update queue
        driverService.addDriver(chatId);
        QueuePassengerDao tripInfo = passengerQueueService.getNextFree(chatId);
        if (tripInfo == null) {
            return menuSender.sendDriverActiveMenu(chatId, Constants.NO_TRIPS_MESSAGE);
        }
        User passengerUserInfo = userInfo.get(tripInfo.getPassengerChatId());
        String message = String.format("%s %s шукає транспорт з вокзалу на вул. %s \n\n%s",
                passengerUserInfo.getFirstName(), passengerUserInfo.getLastName(),
                tripInfo.getAddress(), tripInfo.getDetails());
        return menuSender.sendDriverActiveMenu(chatId, message);
    }

    public SendMessage replyToChooseRolePassenger(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.ENTERING_ADDRESS);
        return menuSender.sendEnterAddressMenu(chatId);
    }

    public SendMessage replyToStopBroadcast(long chatId) throws TelegramApiException{
//        chatStates.put(chatId, State.DRIVER_INACTIVE);
        chatStates.put(chatId, State.CHOOSING_ROLE);
        driverService.removeDriver(chatId);
//        menuSender.sendDriverInactiveMenu(chatId);
        return menuSender.sendChooseRoleMenu(chatId);
    }

    private SendMessage replyToEnterAddress(long chatId, String address) throws TelegramApiException {
        userAddress.put(chatId, address);
        chatStates.put(chatId, State.ENTERING_DETAILS);
        return menuSender.sendEnterDetailsMenu(chatId);
    }

    private SendMessage replyToEnterDetails(long chatId, String details) throws TelegramApiException {
        userDetails.put(chatId, details);
        // TODO: username or phone may be absent
        // TODO: "please allow your phone info, or enter your phone"
        chatStates.put(chatId, State.ENTERING_ON_STATION);
        return menuSender.sendEnterOnStationMenu(chatId);
    }

    private SendMessage replyToNotOnStation(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.CHECKING_OUT_ON_STATION);
        return menuSender.sendCheckingOutOnStationMenu(chatId);
    }

    private SendMessage replyToOnStation(long chatId, Update upd) throws TelegramApiException {
        chatStates.put(chatId, State.APPROVING_TRIP);
        return menuSender.sendApprovingTripMenu(chatId, userAddress.get(chatId), userDetails.get(chatId), upd);
    }

    public SendMessage replyToApproveAddress(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.LOOKING_FOR_DRIVER);
        passengerQueueService.add(new QueuePassengerDao(chatId, userAddress.get(chatId), userDetails.get(chatId)));
        return menuSender.sendAddressApprovedMenu(chatId);
    }

    private String generateDriverOfferTripMessage(QueuePassengerDao queuePassengerDao) {
        if (queuePassengerDao == null)
            return Constants.NO_TRIPS_MESSAGE;

        User user = userInfo.get(queuePassengerDao.getPassengerChatId());
        return String.format("%s %s шукає транспорт з вокзалу на вул. %s \n\n%s",
                 user.getFirstName(), user.getLastName(),
                // todo: exception
                queuePassengerDao.getAddress(), queuePassengerDao.getDetails());
    }

    private String generateDriverTookTripMessage(long chatId, QueuePassengerDao queuePassengerDao) {
        return String.format("%s шукає транспорт з вокзалу на вул. %s \n\n%s",
                userInfo.get(chatId).getFirstName(), queuePassengerDao.getAddress(),
                queuePassengerDao.getDetails());
    }

//    private void onToCancelTrip(long chatId, User user, String address) throws TelegramApiException {
    private SendMessage replyToCancelTrip(long chatId) throws TelegramApiException {
//        DeleteMessage deleteMessage = new DeleteMessage();
//
//        for (Long driver : driversList) {
//            deleteMessage.setChatId(String.valueOf(driver));
//            deleteMessage.setMessageId(1);
//
//            sender.execute(deleteMessage);
//        }

        chatStates.put(chatId, State.ENTERING_ADDRESS);
        replyWithText(chatId, Constants.TRIP_CANCELED_SUCCESS_MESSAGE);

        return menuSender.sendEnterAddressMenu(chatId);
    }

    private void deleteLastBotMessage(long chatId) throws TelegramApiException {
        Integer botMessageToUpdate = updateMessageService.getBotMessageToUpdate(chatId);
        if (botMessageToUpdate != null) {
            DeleteMessage deleteLastBotMessage = DeleteMessage.builder()
                    .messageId(botMessageToUpdate)
                    .chatId(String.valueOf(chatId))
                    .build();
            sender.execute(deleteLastBotMessage);
        }
    }

    private SendMessage replyWithText(long chatId, String messageText) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .build();
    }
}
