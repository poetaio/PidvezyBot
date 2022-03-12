package bots;

import bots.utils.Constants;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.DriverService;
import services.DriverUpdateMessageService;

/**
 * Contains methods which combine messages and corresponding menus and send them to user
 */
public class MenuSender {

    private final MessageSender sender;

    public MenuSender(MessageSender sender) {
        this.sender = sender;
    }

    public void sendChooseRoleMenu(long chatId) throws TelegramApiException {
        sendTextAndMenu(chatId, Constants.CHOOSE_ROLE_REPLY, KeyboardFactory.chooseRoleReplyKeyboard());
    }

    public void sendDriverActiveMenu(long chatId, String message) throws TelegramApiException {
        if (message.equals(Constants.NO_TRIPS_MESSAGE))
            sendTextAndMenu(chatId, message, KeyboardFactory.noTripsReplyMarkup());
        else
            sendTextAndMenu(chatId, message, KeyboardFactory.driverActiveReplyMarkup());

        // if message is present, delete, send new and reset
        // otherwise send new and reset
//        Integer messageToUpdateId = DriverService.getInstance().getMessageToUpdateId(chatId);

//        if (messageToUpdateId != null) {
//            DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatId), messageToUpdateId);
////             Message m = sender.execute(deleteMessage);
//        }

//        SendMessage sendMessage = SendMessage.builder()
//                .chatId(String.valueOf(chatId))
//                .text(message)
//                .build();

//        DriverService.getInstance().putMessageToUpdateId(chatId, sendMessage.ge);

//        if (message.equals(Constants.NO_TRIPS_MESSAGE))
//            sendMessage.setReplyMarkup(KeyboardFactory.noTripsReplyMarkup());
//        else
//            sendMessage.setReplyMarkup(KeyboardFactory.driverActiveReplyMarkup());
    }

    public void sendDriverTookTripMenu(long chatId, User user) throws TelegramApiException {
        String userWaitsForYourCallMessage = String.format("%s Чекає на ваше повідомлення\n%s", user.getFirstName(), user.getUserName());
        sendTextAndMenu(chatId, userWaitsForYourCallMessage, KeyboardFactory.driverTookTripReplyKeyboard());
    }

    public void sendDriverInactiveMenu(long chatId) throws TelegramApiException {
        sendTextAndMenu(chatId, Constants.BROADCAST_STOPPED_TEXT, KeyboardFactory.resumeBroadcastReplyMarkup());
    }

    public void sendEnterAddressMenu(long chatId) throws TelegramApiException {
        sendTextAndMenu(chatId, Constants.ENTER_ADDRESS, KeyboardFactory.enterAddressReplyKeyboard());
    }

    public void sendApprovingTripMenu(long chatId, String address, String details, Update upd) throws TelegramApiException {
        User user = AbilityUtils.getUser(upd);
        String checkFromString = String.format("%s %s шукає транспорт з вокзалу на вул. %s\n\n%s\n\n%s", user.getFirstName(), user.getLastName(), address, details, user.getUserName());
        sendTextAndMenu(chatId, checkFromString, KeyboardFactory.approveAddressReplyKeyboard());
    }

    public void sendEnterDetailsMenu(long chatId) throws TelegramApiException {
        sendTextAndMenu(chatId, Constants.ENTER_DETAILS, KeyboardFactory.enterDetailsReplyKeyboard());
    }

    public void sendCheckingOutOnStationMenu(long chatId) throws TelegramApiException {
        sendTextAndMenu(chatId, Constants.CHECKING_OUT_ON_STATION_MESSAGE, KeyboardFactory.checkingOutOnStationReplyKeyboard());
    }

    public void sendEnterOnStationMenu(long chatId) throws TelegramApiException {
        sendTextAndMenu(chatId, Constants.ARE_YOU_ON_STATION, KeyboardFactory.enterOnStationReplyKeyboard());
    }

    private void sendTextAndMenu(long chatId, String messageText, ReplyKeyboardMarkup menu) throws TelegramApiException {
        sender.execute(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .replyMarkup(menu)
                .build());
    }
}
