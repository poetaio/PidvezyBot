package bots.factories;

import services.EscapeMessageService;
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

/**
 * Combines messages and corresponding menus from ReplyMarkupFactory
 */
public class SendMessageFactory {
    public static SendMessage chooseRoleSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.CHOOSE_ROLE_REPLY, ReplyMarkupFactory.chooseRoleReplyKeyboard());
    }

    public static SendMessage driverActiveSendMessage(long chatId, String message) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(message)
                .parseMode(ParseMode.MARKDOWNV2)
                .build();

        if (message.equals(Constants.NO_TRIPS_MESSAGE))
            sendMessage.setReplyMarkup(ReplyMarkupFactory.noTripsReplyMarkup());
        else
            sendMessage.setReplyMarkup(ReplyMarkupFactory.driverActiveReplyMarkup());
        return sendMessage;
    }

    public static SendMessage noticingPassengerDriverTookTripSendMessage(long chatId, @NotNull User driver, String number) throws TelegramApiException {
        if (number == null) {
            number = "";
        } else if (number.indexOf('+') != 0 && number.length() > 0) {
            number = '+' + number;
        }
        String username = driver.getUserName();
        if (username == null) {
            if (number.equals("")) {
                username = "";
            } else {
                username = "https://t.me/" + number;
            }
        } else {
            username = "@" + username;
        }
//        String message = String.format(Constants.PASSENGER_TRIP_WAS_TAKEN, driver.getFirstName(), username, number);
        String message = EscapeMessageService.escapeMessage(Constants.PASSENGER_TRIP_WAS_TAKEN, driver.getFirstName(), username, number);
        return makeSendMessage(chatId, message);
    }

    public static SendMessage askingPassengerToInformAboutTripSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.INFORM_US_TRIP_STATUS, ReplyMarkupFactory.passengerConfirmingTakingHimReplyKeyboard());
    }

    public static SendMessage askingDriverToInformAboutEndOfTripSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.APPROVING_FINISH_TRIP, ReplyMarkupFactory.driverConfirmingFinishingTripReplyKeyboard());
    }

    public static SendMessage goodBoySendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.GOOD_BOY, ReplyMarkupFactory.passengerThanksReplyKeyboard());
    }

    public static SendMessage wishAGoodTripSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.HAVE_A_NICE_TRIP, ReplyMarkupFactory.passengerThanksReplyKeyboard());
    }

    public static SendMessage returnToSearchingSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.START_SEARCHING_AGAIN);
    }

    public static SendMessage tripAlreadyTakenSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.TRIP_TAKEN_MESSAGE, ReplyMarkupFactory.driverActiveReplyMarkup());
    }

    public static SendMessage driverTookTripSendMessage(long chatId, @NotNull User user, String address, String details, String number) throws TelegramApiException {
        if (number == null) {
            number = "";
        } else if (number.indexOf('+') != 0 && number.length() > 0) {
            number = '+' + number;
        }
        String username = user.getUserName();
        if (username == null) {
            if (number.equals("")) {
                username = "";
            } else {
                username = "https://t.me/" + number;
            }
        } else {
            username = "@" + username;
        }
//        String userWaitsForYourCallMessage = String.format(Constants.IS_WAITING_FOR_A_CALL_MESSAGE, user.getFirstName(), username,
//                number, address, details);
        String userWaitsForYourCallMessage = EscapeMessageService.escapeMessage(Constants.IS_WAITING_FOR_A_CALL_MESSAGE, user.getFirstName(), username,
                number, address, details);
        return makeSendMessage(chatId, userWaitsForYourCallMessage, ReplyMarkupFactory.driverTookTrip());
    }

    public static SendMessage driverInactiveSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.BROADCAST_STOPPED_TEXT, ReplyMarkupFactory.resumeBroadcastReplyMarkup());
    }

    public static SendMessage enterAddressSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_ADDRESS, ReplyMarkupFactory.enterAddressReplyKeyboard());
    }

    public static SendMessage approvingTripSendMessage(long chatId, String address, String details, String number, Update upd) throws TelegramApiException {
        User user = AbilityUtils.getUser(upd);
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (number == null) {
            number = "";
        } else if (number.indexOf('+') != 0 && number.length() > 0) {
            number = '+' + number;
        }
        String username = user.getUserName();
        if (username == null) {
            if (number.equals("")) {
                username = "";
            } else {
                username = "https://t.me/" + number;
            }
        } else {
            username = "@" + username;
        }
        if (currentHour >= Constants.CURFEW_START_HOUR || currentHour <= Constants.CURFEW_END_HOUR) {
            return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.APPROVE_MESSAGE, user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "", address, details,
                    username, number), ReplyMarkupFactory.approveAddressReplyKeyboard());
        }
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.APPROVE_MESSAGE_CURFEW, user.getFirstName(), user.getLastName() != null ? " " + user.getLastName() : "", address, details,
                username, number), ReplyMarkupFactory.tryAgainDuringCurfewReplyKeyboard());
    }

    public static SendMessage enterDetailsSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_DETAILS, ReplyMarkupFactory.enterDetailsReplyKeyboard());
    }

    public static SendMessage enterNumberSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ENTER_NUMBER_MESSAGE, ReplyMarkupFactory.enterNumberReplyKeyboard());
    }

    public static SendMessage editAddressSendMessage(long chatId, String oldAddress) throws TelegramApiException {
//        return makeSendMessage(chatId, String.format(Constants.EDIT_ADDRESS, oldAddress), ReplyMarkupFactory.editAddressReplyKeyboard());
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_ADDRESS, oldAddress), ReplyMarkupFactory.editAddressReplyKeyboard());
    }

    public static SendMessage editDetailsSendMessage(long chatId, String oldDetails) throws TelegramApiException {
//        return makeSendMessage(chatId, String.format(Constants.EDIT_DETAILS, oldDetails), ReplyMarkupFactory.editDetailsReplyKeyboard());
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.EDIT_DETAILS, oldDetails), ReplyMarkupFactory.editDetailsReplyKeyboard());
    }

    public static SendMessage checkingOutOnStationSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.CHECKING_OUT_ON_STATION_MESSAGE, ReplyMarkupFactory.checkingOutOnStationReplyKeyboard());
    }

    public static SendMessage enterOnStationSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.ARE_YOU_ON_STATION, ReplyMarkupFactory.enterOnStationReplyKeyboard());
    }

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
        if (number != null && number.indexOf('+') != 0) {
            number = '+' + number;
        }
//        return makeSendMessage(chatId, String.format(Constants.SEARCH_STOPPED_MESSAGE, user.getFirstName(),
//                user.getLastName() == null ? "" : " " + user.getLastName(), address,
//                details, number), ReplyMarkupFactory.searchStoppedReplyMenu());
        return makeSendMessage(chatId, EscapeMessageService.escapeMessage(Constants.SEARCH_STOPPED_MESSAGE, user.getFirstName(),
                user.getLastName() == null ? "" : " " + user.getLastName(), address,
                details, number), ReplyMarkupFactory.searchStoppedReplyMenu());
    }

    public static SendMessage addressApprovedSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.REQUEST_SENT_EXTENDED_MESSAGE, ReplyMarkupFactory.lookingForDriverReplyMenu());
    }

    public static SendMessage haveANiceTripSendMessage(long chatId) throws TelegramApiException {
        return makeSendMessage(chatId, Constants.HAVE_A_NICE_TRIP, ReplyMarkupFactory.haveANiceTripReplyMenu());
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
}
