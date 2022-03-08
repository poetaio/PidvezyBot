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

    public void sendEnterFromAddress(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, Constants.ENTER_FROM_ADDRESS, KeyboardFactory.enterFromAddressReplyKeyboard());
    }

    private void replyWithTextAndMenu(long chatId, String messageText, ReplyKeyboardMarkup menu) throws TelegramApiException {
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.enableMarkdown(true);
//        sendMessage.setText(messageText);
//        sendMessage.setChatId(String.valueOf(chatId));
//        sendMessage.setReplyMarkup(menu);

        sender.execute(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .replyMarkup(menu)
                .build());
    }

    public void sendStopBroadcastMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, "Сєва, придумай тєкст, пліз, без нього нот воркінг (1)", KeyboardFactory.stopBroadcastReplyMarkup());
    }

    public void sendResumeBroadcastMenu(long chatId) throws TelegramApiException {
        replyWithTextAndMenu(chatId, Constants.BROADCAST_STOPPED_TEXT, KeyboardFactory.resumeBroadcastReplyMarkup());
    }
}
