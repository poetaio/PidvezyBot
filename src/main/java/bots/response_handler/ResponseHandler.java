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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.DriverService;
import services.PassengerQueueService;

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

    // TODO: divide handler
    private final Map<Long, Role> userRole;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;
        menuSender = new MenuSender(sender);

        chatStates = new HashMap<>();
        userAddress = new HashMap<>();
        userDetails = new HashMap<>();
        userInfo = new HashMap<>();

        passengerQueueService = PassengerQueueService.getInstance();

        // TODO: divide handler
        userRole = new HashMap<>();
        setupDriverScheduler();
    }

    private void setupDriverScheduler() {
        new Thread(new DriverMessageScheduler() {
            @Override
            protected void sendNoTripsAvailable(long chatId) throws TelegramApiException {
                menuSender.sendDriverActiveMenu(chatId, Constants.NO_TRIPS_MESSAGE);
            }
            @Override
            protected void sendTripOffer(long chatId, QueuePassengerDao passengerDao) throws TelegramApiException {
                menuSender.sendDriverActiveMenu(chatId, generateDriverOfferTripMessage(chatId, passengerDao));
            }
        }).start();
    }

    /**
     * Replies to /start command with two roles to choose from
     * @param chatId of the chat message comes from
     */
    public void replyToStart(long chatId) throws TelegramApiException{
        chatStates.put(chatId, State.CHOOSING_ROLE);
        menuSender.sendChooseRoleMenu(chatId);
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

        String message = upd.getMessage().getText();

        switch (chatStates.get(chatId)) {
            case CHOOSING_ROLE:
                userInfo.putIfAbsent(chatId, AbilityUtils.getUser(upd));
                onChoosingRole(chatId, message);
                break;

            // Driver states
            case DRIVER_ACTIVE:
                onDriverActive(chatId, message);
                break;
            case DRIVER_TOOK_TRIP:
                onDriverTookTrip(chatId, message, upd);
//            case DRIVER_INACTIVE:
//                onDriverInactive(chatId, message);
//                break;

            // Passenger states
            case ENTERING_ADDRESS:
                onEnteringAddress(chatId, message);
                break;
            case ENTERING_DETAILS:
                onEnteringDetails(chatId, message);
                break;
            case ENTERING_ON_STATION:
                onEnteringOnStation(chatId, message, upd);
                break;
            case CHECKING_OUT_ON_STATION:
                onCheckingOutOnStation(chatId, message, upd);
                break;
            case APPROVING_TRIP:
                onApprovingTrip(chatId, message, upd);
                break;
            case LOOKING_FOR_DRIVER:
                onLookingForDriver(chatId, message);
        }
    }

    /**
     * Choosing role handler
     * @param chatId user chat id
     * @param message message sent by user
     * @throws TelegramApiException Classic telegram exception
     */
    private void onChoosingRole(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.CHOOSE_ROLE_DRIVER:
                replyToChooseRoleDriver(chatId);
                break;
            case Constants.CHOOSE_ROLE_PASSENGER:
                replyToChooseRolePassenger(chatId);
                break;
            default:
                menuSender.sendChooseRoleMenu(chatId);
        }
    }

    /**
     * Driver role choosing handler, subscribe to getting trip applications
     * @param chatId user chat Id
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private void onDriverActive(long chatId, String message) throws TelegramApiException {
        switch (message) {
//            case Constants.STOP_BROADCAST:
//                replyToStopBroadcast(chatId);
//                break;
            case Constants.NEXT_TRIP:
                menuSender.sendDriverActiveMenu(chatId, generateDriverOfferTripMessage(chatId,
                        passengerQueueService.getNextFree(chatId)));
                driverService.resetDriverTime(chatId);
                break;
            case Constants.TAKE_TRIP:
                QueuePassengerDao driverPassenger = passengerQueueService.getPassengerDaoByDriver(chatId);
                if (driverPassenger == null) {
                    // TODO: move logic to separate method (show no trips available or next trip)
                    menuSender.sendDriverActiveMenu(chatId, Constants.NO_TRIPS_MESSAGE);
                    break;
                }
                menuSender.sendDriverTookTripMenu(chatId,
                        userInfo.get(driverPassenger.getPassengerChatId()));
                // TODO: implement removing from queue logic, canceling taking, etc...
                passengerQueueService.remove(chatId);
                chatStates.put(chatId, State.DRIVER_TOOK_TRIP);
                break;
            case Constants.BACK:
                chatStates.put(chatId, State.CHOOSING_ROLE);
                driverService.removeDriver(chatId);
                menuSender.sendChooseRoleMenu(chatId);
                break;
            default:
                // TODO: move logic to separate method (show no trips available or next trip)
                menuSender.sendDriverActiveMenu(chatId,
                        generateDriverTookTripMessage(chatId,
                                passengerQueueService.getPassengerDaoByDriver(chatId)));
                break;
        }
    }

    private void onDriverTookTrip(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.BACK:
                replyWithText(chatId, "Not implemented");
                break;
            default:
                menuSender.sendDriverTookTripMenu(chatId, userInfo.get(
                        passengerQueueService.getPassengerDaoByDriver(chatId).getPassengerChatId()));
        }
    }

    /**
     * Unsubscribe from getting trip applications
     * @param chatId
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private void onDriverInactive(long chatId, String message) throws TelegramApiException {
        switch (message) {
            case Constants.RESUME_BROADCAST:
                replyToChooseRoleDriver(chatId);
                break;
            case Constants.BACK:
                // TODO: refactor move to separate method
                chatStates.put(chatId, State.CHOOSING_ROLE);
                menuSender.sendChooseRoleMenu(chatId);
                break;
            default:
                menuSender.sendDriverInactiveMenu(chatId);
        }
    }

    /**
     * Entering passenger destination address handler,
     * if entered passenger address is valid saves it for future trip
     * @param chatId
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private void onEnteringAddress(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            // TODO: refactor move to separate method
            chatStates.put(chatId, State.CHOOSING_ROLE);
            menuSender.sendChooseRoleMenu(chatId);
        } else if (!message.isEmpty() && !message.isBlank()) {
            replyToEnterAddress(chatId, message);
        } else {
            menuSender.sendEnterAddressMenu(chatId);
        }
    }

    /**
     * Entering passenger destination address handler,
     * if entered passenger details are valid saves them for future trip
     * @param chatId
     * @param message
     * @throws TelegramApiException Classic telegram exception
     */
    private void onEnteringDetails(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.BACK)) {
            // TODO: refactor move to separate method
            chatStates.put(chatId, State.ENTERING_ADDRESS);
            menuSender.sendEnterAddressMenu(chatId);
        } else if (!message.isEmpty() && !message.isBlank()) {
            replyToEnterDetails(chatId, message);
        } else {
            menuSender.sendEnterDetailsMenu(chatId);
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
    private void onEnteringOnStation(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.ON_STATION_NO:
                replyToNotOnStation(chatId);
                break;
            case Constants.ON_STATION_YES:
                replyToOnStation(chatId, upd);
                break;
            case Constants.BACK:
                chatStates.put(chatId, State.ENTERING_DETAILS);
                menuSender.sendEnterDetailsMenu(chatId);
                break;
            default:
                menuSender.sendEnterOnStationMenu(chatId);
        }
    }

    /**
     * Handles passenger's checking out on station after choosing no in "Are you on station" menu
     * @param chatId
     * @param message
     * @param upd
     * @throws TelegramApiException Classic telegram exception
     */
    private void onCheckingOutOnStation(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.I_AM_ON_STATION:
                replyToOnStation(chatId, upd);
                break;
            case Constants.BACK:
                chatStates.put(chatId, State.ENTERING_ON_STATION);
                menuSender.sendEnterOnStationMenu(chatId);
                break;
            default:
                menuSender.sendCheckingOutOnStationMenu(chatId);
        }
    }

    private void onApprovingTrip(long chatId, String message, Update upd) throws TelegramApiException {
        switch (message) {
            case Constants.APPROVE_TRIP:
                replyToApproveAddress(chatId, upd);
                break;
            case Constants.CHANGE_TRIP_INFO:
                // TODO: save or delete current address and details
                chatStates.put(chatId, State.ENTERING_ADDRESS);
                menuSender.sendEnterAddressMenu(chatId);
                break;
            case Constants.BACK:
                // TODO: refactor move to separate method
                chatStates.put(chatId, State.ENTERING_DETAILS);
                menuSender.sendEnterDetailsMenu(chatId);
                break;
            default:
                // TODO: refactor move to separate method
                menuSender.sendApprovingTripMenu(chatId, userAddress.get(chatId), userDetails.get(chatId), upd);
        }
    }

    private void onLookingForDriver(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.CANCEL_TRIP)) {
            // TODO: track trip request recipients to delete it from every chat
            replyToCancelTrip(chatId);
        } else {
            replyWithText(chatId, Constants.REQUEST_PENDING_MESSAGE);
        }
    }

    public void replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.DRIVER_ACTIVE);
        // adding driver to both drivers list and update queue
        driverService.addDriver(chatId);
        QueuePassengerDao tripInfo = passengerQueueService.getNextFree(chatId);
        if (tripInfo == null) {
            menuSender.sendDriverActiveMenu(chatId, Constants.NO_TRIPS_MESSAGE);
            return;
        }
        User passengerUserInfo = userInfo.get(tripInfo.getPassengerChatId());
        String message = String.format("%s %s шукає транспорт з вокзалу на вул. %s \n\n%s",
                passengerUserInfo.getFirstName(), passengerUserInfo.getFirstName(),
                tripInfo.getAddress(), tripInfo.getDetails());
        menuSender.sendDriverActiveMenu(chatId, message);
    }

    public void replyToChooseRolePassenger(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.ENTERING_ADDRESS);
        menuSender.sendEnterAddressMenu(chatId);
    }

    public void replyToStopBroadcast(long chatId) throws TelegramApiException{
//        chatStates.put(chatId, State.DRIVER_INACTIVE);
        chatStates.put(chatId, State.CHOOSING_ROLE);
        driverService.removeDriver(chatId);
//        menuSender.sendDriverInactiveMenu(chatId);
        menuSender.sendChooseRoleMenu(chatId);
    }

    private void replyToEnterAddress(long chatId, String address) throws TelegramApiException {
        userAddress.put(chatId, address);
        chatStates.put(chatId, State.ENTERING_DETAILS);
        menuSender.sendEnterDetailsMenu(chatId);
    }

    private void replyToEnterDetails(long chatId, String details) throws TelegramApiException {
        userDetails.put(chatId, details);
        // TODO: username or phone may be absent
        // TODO: "please allow your phone info, or enter your phone"
        chatStates.put(chatId, State.ENTERING_ON_STATION);
        menuSender.sendEnterOnStationMenu(chatId);
    }

    private void replyToNotOnStation(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.CHECKING_OUT_ON_STATION);
        menuSender.sendCheckingOutOnStationMenu(chatId);
    }

    private void replyToOnStation(long chatId, Update upd) throws TelegramApiException {
        chatStates.put(chatId, State.APPROVING_TRIP);
        menuSender.sendApprovingTripMenu(chatId, userAddress.get(chatId), userDetails.get(chatId), upd);
    }

    public void replyToApproveAddress(long chatId, Update upd) throws TelegramApiException {
        chatStates.put(chatId, State.LOOKING_FOR_DRIVER);
        replyToFindCarButton(chatId, userAddress.get(chatId), userDetails.get(chatId));
    }

    public void replyToFindCarButton(long chatId, String address, String details) throws TelegramApiException {
        passengerQueueService.add(new QueuePassengerDao(chatId, address, details));
    }

    private String generateDriverOfferTripMessage(long chatId, QueuePassengerDao queuePassengerDao) {
        if (queuePassengerDao == null)
            return Constants.NO_TRIPS_MESSAGE;

        User user = userInfo.get(chatId);
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

//    private void replyToCancelTrip(long chatId, User user, String address) throws TelegramApiException {
    private void replyToCancelTrip(long chatId) throws TelegramApiException {
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

        menuSender.sendEnterAddressMenu(chatId);
    }

    private void replyWithText(long chatId, String messageText) throws TelegramApiException {
        sender.execute(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .build());
    }
}
