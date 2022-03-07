package bots;

import bots.utils.Constants;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {
    public static ReplyKeyboard chooseRoleKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowLine = new ArrayList<>();

        InlineKeyboardButton driverRoleButton = new InlineKeyboardButton();
        driverRoleButton.setText(Constants.ROLE_DRIVER);
        driverRoleButton.setCallbackData(Constants.ROLE_DRIVER);

        InlineKeyboardButton passengerRoleButton = new InlineKeyboardButton();
        passengerRoleButton.setText(Constants.ROLE_PASSENGER);
        passengerRoleButton.setCallbackData(Constants.ROLE_PASSENGER);

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
        return null;
    }

    public static ReplyKeyboardMarkup findDriverReplyKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(true);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow findDriverButtonRow = new KeyboardRow();
        findDriverButtonRow.add(Constants.FIND_DRIVER_BUTTON);

        KeyboardRow cancelButtonRow = new KeyboardRow();
        KeyboardButton cancelButton = new KeyboardButton();
        cancelButton.setText(Constants.CANCEL_BUTTON);
        cancelButtonRow.add(cancelButton);

        keyboard.add(findDriverButtonRow);
        keyboard.add(cancelButtonRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
