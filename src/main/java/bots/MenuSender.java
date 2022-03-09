package bots;

import bots.utils.Constants;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MenuSender {

    private final MessageSender sender;

    public MenuSender(MessageSender sender) {
        this.sender = sender;
    }

    public void sendChooseRoleMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, Constants.CHOOSE_ROLE_REPLY, KeyboardFactory.chooseRoleReplyKeyboard());
    }

    public void sendDriverActiveMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, "Сєва, придумай тєкст, пліз, без нього нот воркінг (1)", KeyboardFactory.stopBroadcastReplyMarkup());
    }

    public void sendDriverInactiveMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, Constants.BROADCAST_STOPPED_TEXT, KeyboardFactory.resumeBroadcastReplyMarkup());}

    public void sendChooseTripTypeMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, "Сєва, придумай тєкст, пліз, без нього нот воркінг (2)", KeyboardFactory.chooseTripTypeReplyKeyboard());
    }

    public void sendEnterFromAddressMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, Constants.ENTER_FROM_ADDRESS, KeyboardFactory.enterFromAddressReplyKeyboard());
    }

    public void sendEnterToAddressMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, Constants.ENTER_TO_ADDRESS, KeyboardFactory.enterToAddressReplyKeyboard());
    }

    private void replyWithTextAndMenu(long chatId, String messageText, ReplyKeyboardMarkup menu) throws TelegramApiException {
        sender.execute(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .replyMarkup(menu)
                .build());
    }
}
