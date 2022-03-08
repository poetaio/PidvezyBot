package bots;

import bots.utils.Constants;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {
    public static ReplyKeyboard chooseRoleKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowLine = new ArrayList<>();

        InlineKeyboardButton driverRoleButton = new InlineKeyboardButton();
        driverRoleButton.setText(Constants.CHOOSE_ROLE_DRIVER);
        driverRoleButton.setCallbackData(Constants.CHOOSE_ROLE_DRIVER);

        InlineKeyboardButton passengerRoleButton = new InlineKeyboardButton();
        passengerRoleButton.setText(Constants.CHOOSE_ROLE_PASSENGER);
        passengerRoleButton.setCallbackData(Constants.CHOOSE_ROLE_PASSENGER);

        rowLine.add(driverRoleButton);
        rowLine.add(passengerRoleButton);

        rowsInline.add(rowLine);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public static ReplyKeyboard findDriverKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> menu = new ArrayList<>();
        List<InlineKeyboardButton> findDriverButtonLine = new ArrayList<>();

        InlineKeyboardButton findDriverButton = new InlineKeyboardButton();
        findDriverButton.setText("Find Driver");
        findDriverButton.setCallbackData("Find Driver");

        findDriverButtonLine.add(findDriverButton);
        menu.add(findDriverButtonLine);
        markup.setKeyboard(menu);
        return markup;
    }

    public static ReplyKeyboardMarkup chooseRoleReplyKeyboard() {
        return makeOneColumnMenu(Constants.CHOOSE_ROLE_DRIVER, Constants.CHOOSE_ROLE_PASSENGER);
    }

    // driver menus
    public static ReplyKeyboardMarkup resumeBroadcastReplyMarkup() {
        return makeOneColumnMenu(Constants.RESUME_BROADCAST, Constants.CANCEL);
    }

    public static ReplyKeyboardMarkup stopBroadcastReplyMarkup() {
        return makeOneColumnMenu(Constants.STOP_BROADCAST, Constants.CANCEL);
    }

    // passenger menus
    public static ReplyKeyboardMarkup chooseDestinationTypeReplyKeyboard() {
        return makeOneColumnMenu(Constants.CHOOSE_FROM_STATION, Constants.CHOOSE_TO_STATION, Constants.CANCEL);
    }

    public static ReplyKeyboardMarkup enterToAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.ENTER_TO_ADDRESS, Constants.CANCEL);
    }

    public static ReplyKeyboardMarkup enterFromAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.ENTER_TO_ADDRESS, Constants.CANCEL);
    }

    public static ReplyKeyboardMarkup approveAddressReplyKeyboard() {
        return makeOneColumnMenu(Constants.APPROVE_ADDRESS, Constants.CANCEL);
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
