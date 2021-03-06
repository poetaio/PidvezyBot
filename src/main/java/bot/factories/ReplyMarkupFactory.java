package bot.factories;

import bot.utils.Constants;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates all menus layouts
 */
public class ReplyMarkupFactory {
    public static ReplyKeyboardMarkup chooseRoleReplyKeyboard() {
        return makeOneColumnMenu(Constants.CHOOSE_ROLE_DRIVER, Constants.CHOOSE_ROLE_PASSENGER, Constants.FAQ);
    }

    public static ReplyKeyboardMarkup faqReplyKeyboard() {
        return makeOneColumnMenu(Constants.HOW_TO_GET_PERMIT, Constants.BACK);
    }

    public static ReplyKeyboardMarkup permitReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }

    // driver menus
    public static ReplyKeyboardMarkup resumeBroadcastReplyMarkup() {
        return makeOneColumnMenu(Constants.RESUME_BROADCAST);
//        return makeOneColumnMenu(Constants.RESUME_BROADCAST, Constants.BACK);
    }

    public static ReplyKeyboardMarkup passengerConfirmingTakingHimReplyKeyboard() {
        return makeOneColumnMenu(Constants.FOUND_TRIP, Constants.FIND_AGAIN);
    }

    public static ReplyKeyboardMarkup driverConfirmingFinishingTripReplyKeyboard() {
        return makeOneColumnMenu(Constants.FINISH_TRIP);
    }

    public static ReplyKeyboardMarkup passengerThanksReplyKeyboard() {
        return makeOneColumnMenu(Constants.THANKS);
    }

    public static ReplyKeyboardMarkup driverActiveReplyMarkup() {
        return makeOneColumnMenu(Constants.TAKE_TRIP, Constants.NEXT_TRIP, Constants.STOP_BROADCAST, Constants.BACK);
    }

    public static ReplyKeyboardMarkup noTripsReplyMarkup() {
        return makeOneColumnMenu(Constants.STOP_BROADCAST, Constants.BACK);
    }

    public static ReplyKeyboardMarkup driverTookTrip() {
        return makeOneColumnMenu(Constants.FINISH_TRIP, Constants.DRIVER_DISMISS_TRIP);
    }
    public static ReplyKeyboardMarkup goodBoyReplyKeyboard() throws TelegramApiException {
        return makeOneColumnMenu(Constants.AM_GOOD_BOY);
    }

    // passenger menus

    public static ReplyKeyboardMarkup enterAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }
    // edit address menus

    public static ReplyKeyboardMarkup editAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE, Constants.BACK);
    }

    public static ReplyKeyboardMarkup editAddressApproveReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE);
    }

    public static ReplyKeyboardMarkup editAddressSearchStopReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE);
    }
    // edit details menus

    public static ReplyKeyboardMarkup editDetailsRegularReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE, Constants.BACK);
    }

    public static ReplyKeyboardMarkup editDetailsApproveReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE);
    }

    public static ReplyKeyboardMarkup editDetailsSearchStopReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE);
    }

    public static ReplyKeyboardMarkup enterDetailsReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup enterNumberReplyKeyboard() {
        return makeOneColumnMenu(KeyboardButton.builder()
                .text(Constants.SHARE_NUMBER)
                .requestContact(true)
                .build(), new KeyboardButton(Constants.BACK));
    }

    public static ReplyKeyboardMarkup checkingOutOnStationReplyKeyboard() {
        return makeOneColumnMenu(Constants.I_AM_ON_STATION, Constants.BACK);
    }

    public static ReplyKeyboardMarkup approveAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.APPROVE_TRIP, Constants.EDIT_ADDRESS, Constants.EDIT_DETAILS, Constants.BACK);
    }

    public static ReplyKeyboardMarkup tryAgainDuringCurfewReplyKeyboard() {
        return makeOneColumnMenu(Constants.TRY_AGAIN, Constants.EDIT_ADDRESS, Constants.EDIT_DETAILS, Constants.BACK);
    }

    public static ReplyKeyboardMarkup lookingForDriverReplyMenu() {
        return makeOneColumnMenu(Constants.STOP_LOOKING_FOR_A_CAR);
    }

    public static ReplyKeyboardMarkup searchStoppedReplyMenu() {
        return makeOneColumnMenu(Constants.RESUME_SEARCH, Constants.EDIT_ADDRESS, Constants.EDIT_DETAILS, Constants.CANCEL_TRIP);
    }

    public static ReplyKeyboardMarkup addressApprovedReplyMarkup() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup haveANiceTripReplyMenu() {
        return makeOneColumnMenu(Constants.THANKS);
    }

    private static ReplyKeyboardMarkup makeOneColumnMenu(String... buttons) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (String button : buttons) {
            KeyboardRow buttonRow = new KeyboardRow();
            buttonRow.add(button);
            keyboard.add(buttonRow);
        }
        return ReplyKeyboardMarkup.builder()
                .selective(true)
//                .inputFieldPlaceholder("This text is a placeholder")
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .keyboard(keyboard)
                .build();
    }

    private static ReplyKeyboardMarkup makeOneColumnMenu(KeyboardButton... buttons) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (KeyboardButton button : buttons) {
            KeyboardRow buttonRow = new KeyboardRow();
            buttonRow.add(button);
            keyboard.add(buttonRow);
        }
        return ReplyKeyboardMarkup.builder()
                .selective(true)
//                .inputFieldPlaceholder("This text is a placeholder")
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .keyboard(keyboard)
                .build();
    }
}
