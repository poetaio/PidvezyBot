package bots.factories;

import bots.utils.Constants;
import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Calendar;
import java.util.Date;

/**
 * Combines messages and corresponding menus from ReplyMarkupFactory
 */
public class SendMessageFactory {
    public static SendMessage chooseRoleSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.CHOOSE_ROLE_REPLY, ReplyMarkupFactory.chooseRoleReplyKeyboard());
    }

    public static SendMessage driverActiveSendMessage(long chatId, String message) throws TelegramApiException {
//         if message is present, delete, send new and reset
//         otherwise send new and reset
//        Integer messageToUpdateId = DriverService.getInstance().getMessageToUpdateId(chatId);
//
//        if (messageToUpdateId != null) {
//            DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatId), messageToUpdateId);
//            sender.execute(deleteMessage);
//        }

        SendMessage sendMessage = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(message)
                .parseMode(ParseMode.HTML)
                .build();

        if (message.equals(Constants.NO_TRIPS_MESSAGE))
            sendMessage.setReplyMarkup(ReplyMarkupFactory.noTripsReplyMarkup());
        else
            sendMessage.setReplyMarkup(ReplyMarkupFactory.driverActiveReplyMarkup());
        return sendMessage;
    }

    public static SendMessage tripAlreadyTakenSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.TRIP_TAKEN_MESSAGE, ReplyMarkupFactory.driverActiveReplyMarkup());
    }

    public static SendMessage driverTookTripSendMessage(long chatId, @NotNull User user, String address, String details) throws TelegramApiException {
        String userWaitsForYourCallMessage = String.format(Constants.IS_WAITING_FOR_A_CALL_MESSAGE, user.getFirstName(), user.getUserName(),
                address, details);
        return makeSendMessage(chatId, userWaitsForYourCallMessage, ReplyMarkupFactory.driverTookTripReplyKeyboard());
    }

    public static SendMessage driverInactiveSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.BROADCAST_STOPPED_TEXT, ReplyMarkupFactory.resumeBroadcastReplyMarkup());
    }

    public static SendMessage enterAddressSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_ADDRESS, ReplyMarkupFactory.enterAddressReplyKeyboard());
    }

    public static SendMessage approvingTripSendMessage(long chatId, String address, String details, Update upd) throws TelegramApiException {
        User user = AbilityUtils.getUser(upd);
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (currentHour >= Constants.CURFEW_START_HOUR || currentHour <= Constants.CURFEW_END_HOUR) {
            return makeSendMessage(chatId, String.format(Constants.APPROVE_MESSAGE, user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "", address, details, user.getUserName()),
                    ReplyMarkupFactory.approveAddressReplyKeyboard());
        }
        return makeSendMessage(chatId, String.format(Constants.APPROVE_MESSAGE_CURFEW, user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "", address, details, user.getUserName()), ReplyMarkupFactory.tryAgainDuringCurfewReplyKeyboard());
    }

    public static SendMessage enterDetailsSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_DETAILS, ReplyMarkupFactory.enterDetailsReplyKeyboard());
    }

    public static SendMessage editAddressSendMessage(long chatId, String oldAddress) throws TelegramApiException {
        return makeSendMessage(chatId, String.format(Constants.EDIT_ADDRESS, oldAddress), ReplyMarkupFactory.editAddressReplyKeyboard());
    }

    public static SendMessage editDetailsSendMessage(long chatId, String oldDetails) throws TelegramApiException {
        return makeSendMessage(chatId, String.format(Constants.EDIT_DETAILS, oldDetails), ReplyMarkupFactory.editDetailsReplyKeyboard());
    }

    public static SendMessage checkingOutOnStationSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.CHECKING_OUT_ON_STATION_MESSAGE, ReplyMarkupFactory.checkingOutOnStationReplyKeyboard());
    }

    public static SendMessage enterOnStationSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ARE_YOU_ON_STATION, ReplyMarkupFactory.enterOnStationReplyKeyboard());
    }

    public static SendMessage addressApprovedSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.REQUEST_SENT_MESSAGE, ReplyMarkupFactory.lookingForDriverReplyMenu());
    }

    public static SendMessage haveANiceTripSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.HAVE_A_NICE_TRIP, ReplyMarkupFactory.haveANiceTripReplyMenu());
    }

    private static SendMessage makeSendMessage(long chatId, String messageText, ReplyKeyboardMarkup replyMarkup) throws TelegramApiException {
//        Integer messageId = UpdateMessageService.getInstance().getBotMessageToUpdate(chatId);

//        if (messageId == null)
            return SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .parseMode(ParseMode.HTML)
                    .text(messageText)
                    .replyMarkup(replyMarkup)
                    .build();

//        return EditMessageReplyMarkup.builder()
//                .chatId(String.valueOf(chatId))
//                .messageId(UpdateMessageService.getInstance().getBotMessageToUpdate(chatId))
//                .replyMarkup(markup)
//                .build();
    }
}
