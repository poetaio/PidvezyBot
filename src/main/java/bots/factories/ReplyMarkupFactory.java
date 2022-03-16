package bots.factories;

import bots.utils.Constants;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
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

    public static ReplyKeyboardMarkup driverTookTripReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup driverActiveReplyMarkup() {
        return makeOneColumnMenu(Constants.TAKE_TRIP, Constants.NEXT_TRIP, Constants.BACK);
    }

    public static ReplyKeyboardMarkup noTripsReplyMarkup() {
        return makeOneColumnMenu(Constants.BACK);
    }

    // passenger menus
    public static ReplyKeyboardMarkup enterAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup enterDetailsReplyKeyboard() {
        return makeOneColumnMenu(Constants.BACK);
    }

    public static ReplyKeyboardMarkup enterOnStationReplyKeyboard() {
        return makeOneColumnMenu(Constants.ON_STATION_NO, Constants.ON_STATION_YES, Constants.BACK);
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
        return makeOneColumnMenu(Constants.THANK_YOU);
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
}
