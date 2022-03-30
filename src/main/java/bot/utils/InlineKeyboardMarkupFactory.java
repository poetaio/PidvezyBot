package bot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InlineKeyboardMarkupFactory {
    public static InlineKeyboardMarkup tripLinkKeyboard(String data) {
        return makeInlineKeyboardMarkup(makeInlineKeyboardButton(Constants.TAKE_TRIP, data));
    }

    private static InlineKeyboardMarkup makeInlineKeyboardMarkup(InlineKeyboardButton... buttons) {
        List<InlineKeyboardButton> buttonsList = new LinkedList<>(Arrays.asList(buttons));

        return InlineKeyboardMarkup.builder()
                .keyboardRow(buttonsList)
                .build();
    }

    private static InlineKeyboardButton makeInlineKeyboardButton(String text, String data) {
        return InlineKeyboardButton.builder()
                .text(text)
//                .callbackData(data)
                .url(Constants.PIDVEZY_BOT_URL)
                .build();
    }
}
