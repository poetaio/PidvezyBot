package bots.factories;

import services.EscapeMessageService;
import bots.utils.Constants;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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

    public static SendMessage faqSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.FAQ_MESSAGE, ReplyMarkupFactory.faqReplyKeyboard());
    }

    public static SendMessage driverActiveSendMessage(long chatId, String tripOffer) throws TelegramApiException {
        return makeSendMessage(chatId, tripOffer, ReplyMarkupFactory.driverActiveReplyMarkup());
    }

    public static SendMessage noTripsAvailableSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.NO_TRIPS_MESSAGE, ReplyMarkupFactory.noTripsReplyMarkup());
    }

    public static SendMessage noticingPassengerDriverTookTripSendMessage(long chatId, @NotNull User driver, String number) throws TelegramApiException {
        number = normalizeNumber(number);
        String username = normalizeUsername(driver.getUserName(), number);
        String tripTakenAndDriverInfoMessage = EscapeMessageService.escapeMessage(Constants.PASSENGER_TRIP_WAS_TAKEN, driver.getFirstName(), username, number);
        return makeSendMessage(chatId, tripTakenAndDriverInfoMessage);
    }

    public static SendMessage askingPassengerToInformAboutTripSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.INFORM_US_TRIP_STATUS, ReplyMarkupFactory.passengerConfirmingTakingHimReplyKeyboard());
    }

    public static SendMessage goodBoySendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.THANKS, ReplyMarkupFactory.goodBoyReplyKeyboard());
    }

    public static SendMessage tripAlreadyTakenSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.TRIP_TAKEN_MESSAGE, ReplyMarkupFactory.driverActiveReplyMarkup());
    }

    public static SendMessage driverTookTripSendMessage(long chatId, @NotNull User user, String address, String details, String number) throws TelegramApiException {
        number = normalizeNumber(number);
        String username = normalizeUsername(user.getUserName(), number);
        String userWaitsForYourCallMessage = EscapeMessageService.escapeMessage(Constants.IS_WAITING_FOR_A_CALL_MESSAGE, user.getFirstName(), username,
                number, address, details);
        return makeSendMessage(chatId, userWaitsForYourCallMessage, ReplyMarkupFactory.driverTookTrip());
    }

    public static SendMessage driverTookTripInformUsSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.LET_US_KNOW_WHEN_TRIP_IS_OVER, ReplyMarkupFactory.driverTookTrip());
    }

    public static SendMessage driverInactiveSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.BROADCAST_STOPPED_TEXT, ReplyMarkupFactory.resumeBroadcastReplyMarkup());
    }

    public static SendMessage enterAddressSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_ADDRESS, ReplyMarkupFactory.enterAddressReplyKeyboard());
    }

    public static SendMessage approvingTripSendMessage(long chatId, String address, String details, String number, User user) throws TelegramApiException {
        number = normalizeNumber(number);
        String username = normalizeUsername(user.getUserName(), number);
        String approveMessage = EscapeMessageService.escapeMessage(Constants.APPROVE_MESSAGE, user.getFirstName(),
                user.getLastName() != null ? " " + user.getLastName() : "", address, details, username, number);
        return makeSendMessage(chatId, approveMessage, ReplyMarkupFactory.approveAddressReplyKeyboard());
    }

    public static SendMessage tryAgainDuringCurfewSendMessage(long chatId, String address, String details, String number, User user) throws TelegramApiException {
        number = normalizeNumber(number);
        String username = normalizeUsername(user.getUserName(), number);
        String curfewMessage = EscapeMessageService.escapeMessage(Constants.APPROVE_MESSAGE_CURFEW, user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "", address, details,
                username, number);
        return makeSendMessage(chatId, curfewMessage, ReplyMarkupFactory.tryAgainDuringCurfewReplyKeyboard());
    }

    public static SendMessage enterDetailsSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_DETAILS, ReplyMarkupFactory.enterDetailsReplyKeyboard());
    }

    public static SendMessage passengerEnterNumberSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.PASSENGER_ENTER_NUMBER_MESSAGE, ReplyMarkupFactory.enterNumberReplyKeyboard());
    }

    public static SendMessage driverEnterNumberSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.DRIVER_ENTER_NUMBER_MESSAGE, ReplyMarkupFactory.enterNumberReplyKeyboard());
    }

    public static SendMessage requestPendingMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.REQUEST_PENDING_MESSAGE);
    }

    // edit address send messages
    public static SendMessage editAddressRegularSendMessage(long chatId, String oldAddress) throws TelegramApiException {
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_ADDRESS_MESSAGE, oldAddress), ReplyMarkupFactory.editAddressReplyKeyboard());
    }

    public static SendMessage editAddressApproveSendMessage(long chatId, String tripAddress) throws TelegramApiException {
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_ADDRESS_MESSAGE, tripAddress), ReplyMarkupFactory.editAddressApproveReplyKeyboard());
    }

    public static SendMessage editAddressSearchStopSendMessage(long chatId, String tripAddress) throws TelegramApiException {
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_ADDRESS_MESSAGE, tripAddress), ReplyMarkupFactory.editAddressSearchStopReplyKeyboard());
    }

    // edit details send messages
    public static SendMessage editDetailsRegularSendMessage(long chatId, String oldDetails) throws TelegramApiException {
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_DETAILS_MESSAGE, oldDetails), ReplyMarkupFactory.editDetailsRegularReplyKeyboard());
    }

    public static SendMessage editDetailsApproveSendMessage(long chatId, String oldDetails) throws TelegramApiException {
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_DETAILS_MESSAGE, oldDetails), ReplyMarkupFactory.editDetailsApproveReplyKeyboard());
    }

    public static SendMessage editDetailsSearchStopSendMessage(long chatId, String oldDetails) throws TelegramApiException {
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_DETAILS_MESSAGE, oldDetails), ReplyMarkupFactory.editDetailsSearchStopReplyKeyboard());
    }

    // after approving messages
    public static SendMessage requestSentSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.REQUEST_SENT_MESSAGE, ReplyMarkupFactory.lookingForDriverReplyMenu());
    }

    public static SendMessage searchingForDriversSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.REQUEST_PENDING_MESSAGE, ReplyMarkupFactory.lookingForDriverReplyMenu());
    }

    public static SendMessage driversGotYourMessageSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.DRIVERS_GOT_YOUR_MESSAGE, ReplyMarkupFactory.lookingForDriverReplyMenu());
    }

    public static SendMessage tripSearchStoppedSendMessage(long chatId, User user, String address, String details, String number) throws TelegramApiException {
        number = normalizeNumber(number);
        String stoppedTripInfoMessage = EscapeMessageService.escapeMessage(Constants.SEARCH_STOPPED_MESSAGE, user.getFirstName(),
                user.getLastName() == null ? "" : " " + user.getLastName(), address,
                details, number);
        return makeSendMessage(chatId, stoppedTripInfoMessage, ReplyMarkupFactory.searchStoppedReplyMenu());
    }

    public static SendMessage approveOrDismissTrip(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.chooseRightMenuOptionMessage, ReplyMarkupFactory.driverTookTrip());
    }

    public static SendMessage curfewIsOverSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.CURFEW_IS_OVER_MESSAGE, ReplyMarkupFactory.tryAgainDuringCurfewReplyKeyboard());
    }

    private static SendMessage makeSendMessage(long chatId, String messageText) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .parseMode(ParseMode.MARKDOWNV2)
                .text(messageText)
                .build();
    }

    private static SendMessage makeSendMessage(long chatId, String messageText, ReplyKeyboardMarkup replyMarkup) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .parseMode(ParseMode.MARKDOWNV2)
                .text(messageText)
                .replyMarkup(replyMarkup)
                .build();
    }

    private static String normalizeNumber(String number) {
        if (number == null) {
            return "";
        } else if (number.indexOf('+') != 0 && number.length() > 0) {
            return '+' + number;
        }
        return number;
    }

    // should be called after normalizing number (so that it wouldn't be null)
    private static String normalizeUsername(String username, @NotNull String number) {
        if (username == null) {
            if (number.equals("")) {
                return "";
            }
            return "https://t.me/" + number;
        }
        return "@" + username;
    }
}
