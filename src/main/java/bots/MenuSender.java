package bots;

import bots.utils.Constants;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Contains methods which combine messages and corresponding menus and send them to user
 */
public class MenuSender {

    private final MessageSender sender;

    public MenuSender(MessageSender sender) {
        this.sender = sender;
    }

    public SendMessage sendChooseRoleMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.CHOOSE_ROLE_REPLY, KeyboardFactory.chooseRoleReplyKeyboard());
    }

    public SendMessage sendDriverActiveMenu(long chatId, String message) throws TelegramApiException {
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
            sendMessage.setReplyMarkup(KeyboardFactory.noTripsReplyMarkup());
        else
            sendMessage.setReplyMarkup(KeyboardFactory.driverActiveReplyMarkup());
        return sendMessage;
    }

    public SendMessage sendDriverTookTripMenu(long chatId, User user) throws TelegramApiException {
        String userWaitsForYourCallMessage = String.format("%s Чекає на ваше повідомлення або дзвінок\n@%s", user.getFirstName(), user.getUserName());
        return sendTextAndMenu(chatId, userWaitsForYourCallMessage, KeyboardFactory.driverTookTripReplyKeyboard());
    }

    public SendMessage sendDriverInactiveMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.BROADCAST_STOPPED_TEXT, KeyboardFactory.resumeBroadcastReplyMarkup());
    }

    public SendMessage sendEnterAddressMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.ENTER_ADDRESS, KeyboardFactory.enterAddressReplyKeyboard());
    }

    public SendMessage sendApprovingTripMenu(long chatId, String address, String details, Update upd) throws TelegramApiException {
        User user = AbilityUtils.getUser(upd);
        String checkFromString = String.format("Підтвердіть запит:\n%s %s шукає транспорт з вокзалу на вул. %s\n\n%s\n\n@%s", user.getFirstName(), user.getLastName(), address, details, user.getUserName());
        return sendTextAndMenu(chatId, checkFromString, KeyboardFactory.approveAddressReplyKeyboard());
    }

    public SendMessage sendEnterDetailsMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.ENTER_DETAILS, KeyboardFactory.enterDetailsReplyKeyboard());
    }

    public SendMessage sendCheckingOutOnStationMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.CHECKING_OUT_ON_STATION_MESSAGE, KeyboardFactory.checkingOutOnStationReplyKeyboard());
    }

    public SendMessage sendEnterOnStationMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.ARE_YOU_ON_STATION, KeyboardFactory.enterOnStationReplyKeyboard());
    }

    public SendMessage sendAddressApprovedMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.REQUEST_SENT_MESSAGE, KeyboardFactory.lookingForDriverReplyMenu());
    }

    public SendMessage sendHaveANiceTripMenu(long chatId) throws TelegramApiException {
        return sendTextAndMenu(chatId, Constants.HAVE_A_NICE_TRIP, KeyboardFactory.haveANiceTripReplyMenu());
    }

    private SendMessage sendTextAndMenu(long chatId, String messageText, ReplyKeyboardMarkup menu) throws TelegramApiException {
//        Integer messageId = UpdateMessageService.getInstance().getBotMessageToUpdate(chatId);

//        if (messageId == null)
            return SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(messageText)
                    .replyMarkup(menu)
                    .build();

//        EditMessageText.builder().replyMarkup(menu);
//        EditMessageCaption.builder().replyMarkup(menu);
//        EditMessageMedia.builder().replyMarkup(menu);
//        EditMessageReplyMarkup.builder().replyMarkup(menu);
//        EditMessageLiveLocation.builder().replyMarkup(menu);

//        List<InlineKeyboardButton> buttons = new LinkedList<>();
//        buttons.add(InlineKeyboardButton.builder().text("Something").build());
//        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
//                .keyboardRow(buttons)
//                .build();
//
//        return EditMessageReplyMarkup.builder()
//                .chatId(String.valueOf(chatId))
//                .messageId(UpdateMessageService.getInstance().getBotMessageToUpdate(chatId))
//                .replyMarkup(markup)
//                .build();
    }
}
