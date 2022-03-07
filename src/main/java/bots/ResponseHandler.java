package bots;

import bots.utils.Constants;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.LinkedList;

public class ResponseHandler {
    private final MessageSender sender;
    private final LinkedList<Long> driversList;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;
        driversList = new LinkedList<>();
    }

    public void replyToStart(long chatId) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(Constants.START_REPLY);
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setReplyMarkup(KeyboardFactory.chooseRoleKeyboard());

            sender.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void replyToChooseRoleButtons(long chatId, String buttonId) {
        System.out.println("HERE");
        try {
            switch (buttonId) {
                case Constants.ROLE_DRIVER:
                    replyToChoseDriver(chatId);
                    break;
                case Constants.ROLE_PASSENGER:
                    replyToChosePassenger(chatId);
                    break;
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void replyToChoseDriver(long chatId) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("You chose driver");
        sendMessage.setChatId(String.valueOf(chatId));

        driversList.add(chatId);

        sender.execute(sendMessage);
    }

    private void replyToChosePassenger(long chatId) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setText("You chose passenger");
        sendMessage.setChatId(String.valueOf(chatId));
//        sendMessage.setReplyMarkup(KeyboardFactory.findDriverKeyboard());
        sendMessage.setReplyMarkup(KeyboardFactory.findDriverReplyKeyboard());

        sender.execute(sendMessage);
    }

    public void replyToFindCarButton(long chatId, User user) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(String.format("Passenger %s %s is looking for a car", user.getFirstName(), user.getLastName()));

            for (Long driver : driversList) {
                sendMessage.setChatId(String.valueOf(driver));
                sender.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
