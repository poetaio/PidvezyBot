package bots;

import bots.utils.Constants;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all menus layouts of corresponding states
 */
public class KeyboardFactory {
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

    public static ReplyKeyboard lookingForDriverReplyMenu() {
        return makeOneColumnMenu(Constants.CANCEL_TRIP);
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
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .keyboard(keyboard)
                .build();
    }
}
