package bots.factories;

import bots.utils.Constants;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates all menus layouts
 */
public class ReplyMarkupFactory {
    public static ReplyKeyboardMarkup chooseRoleReplyKeyboard() {
        return makeOneColumnMenu(Constants.CHOOSE_ROLE_DRIVER, Constants.CHOOSE_ROLE_PASSENGER);
    }

    // driver menus
    public static ReplyKeyboardMarkup resumeBroadcastReplyMarkup() {
        return makeOneColumnMenu(Constants.RESUME_BROADCAST, Constants.BACK);
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
        return makeOneColumnMenu(Constants.TAKE_TRIP, Constants.NEXT_TRIP, Constants.BACK);
    }

    public static ReplyKeyboardMarkup noTripsReplyMarkup() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup driverTookTrip() {
        return makeOneColumnMenu(Constants.DRIVER_PIDVEZY, Constants.DRIVER_DISMISS_TRIP);
    }

    // passenger menus
    public static ReplyKeyboardMarkup enterAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup editAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE, Constants.BACK);
    }

    public static ReplyKeyboardMarkup editDetailsReplyKeyboard() {
        return makeOneColumnMenu(Constants.DO_NOT_CHANGE, Constants.BACK);
    }

    public static ReplyKeyboardMarkup enterDetailsReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup enterOnStationReplyKeyboard() {
        return makeOneColumnMenu(Constants.ON_STATION_NO, Constants.ON_STATION_YES, Constants.BACK);
    }

    public static ReplyKeyboardMarkup enterNumberReplyKeyboard() {
        return makeOneColumnMenu(KeyboardButton.builder()
                .text(Constants.ENTER_NUMBER)
                .requestContact(true)
                .build(), new KeyboardButton(Constants.BACK));
    }

    public static ReplyKeyboardMarkup checkingOutOnStationReplyKeyboard() {
        return makeOneColumnMenu(Constants.I_AM_ON_STATION, Constants.BACK);
    }

    public static ReplyKeyboardMarkup approveAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.APPROVE_TRIP, Constants.CHANGE_TRIP_INFO, Constants.BACK);
    }

    public static ReplyKeyboardMarkup tryAgainDuringCurfewReplyKeyboard() {
        return makeOneColumnMenu(Constants.TRY_AGAIN, Constants.CHANGE_TRIP_INFO, Constants.BACK);
    }

    public static ReplyKeyboardMarkup lookingForDriverReplyMenu() {
        return makeOneColumnMenu(Constants.STOP_LOOKING_FOR_A_CAR, Constants.EDIT_TRIP);
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

    public static ReplyKeyboardMarkup searchStoppedReplyMenu() {
        return makeOneColumnMenu(Constants.CANCEL_TRIP, Constants.CHANGE_TRIP_INFO);
    }
}
