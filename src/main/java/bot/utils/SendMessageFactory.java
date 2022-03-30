package bot.utils;

import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class SendMessageFactory {

    public static SendMessage newTripSendMessage(long chatId, String tripOffer) throws TelegramApiException {
        return makeSendMessage(chatId, tripOffer);
    }

    private static SendMessage makeSendMessage(long chatId, String messageText, ReplyKeyboard replyMarkup) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .parseMode(ParseMode.MARKDOWNV2)
                .replyMarkup(replyMarkup)
                .text(messageText)
                .build();
    }

    public static EditMessageText removeTripUpdateMessage(long groupId, Integer messageId, String tripUpdatedMessage) throws TelegramApiException {
        return makeUpdateMessage(groupId, messageId, tripUpdatedMessage);
    }

    private static SendMessage makeSendMessage(long chatId, String messageText) throws TelegramApiException {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .parseMode(ParseMode.MARKDOWNV2)
                .build();
    }

    private static EditMessageText makeUpdateMessage(long chatId, int messageId, String messageText) throws TelegramApiException {
        return EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(messageText)
                .parseMode(ParseMode.MARKDOWNV2)
                .build();
    }

    public static DeleteMessage removeTripDeleteMessage(long groupId, Integer messageId) throws TelegramApiException {
        return DeleteMessage.builder()
                .chatId(String.valueOf(groupId))
                .messageId(messageId)
                .build();
    }
}
