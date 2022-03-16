package bots.factories;

import bots.utils.Constants;
import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
        String userWaitsForYourCallMessage = String.format("%s Чекає на ваше повідомлення або дзвінок\n@%s\n\n%s\n%s", user.getFirstName(), user.getUserName(),
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
        String checkFromString = String.format("Підтвердіть запит:\n%s%s шукає транспорт з вокзалу на вул. %s\n\n%s\n\n@%s", user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "", address, details, user.getUserName());
        return makeSendMessage(chatId, checkFromString, ReplyMarkupFactory.approveAddressReplyKeyboard());
    }

    public static SendMessage enterDetailsSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_DETAILS, ReplyMarkupFactory.enterDetailsReplyKeyboard());
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
