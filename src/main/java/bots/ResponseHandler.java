package bots;

import bots.utils.Constants;
import bots.utils.State;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ResponseHandler {
    private final MessageSender sender;
    private final MenuSender menuSender;

    private final LinkedList<Long> driversList;

    private final Map<Long, State> chatStates;
    private final Map<Long, String> userAddress;

    public ResponseHandler(MessageSender sender) {
        this.sender = sender;
        menuSender = new MenuSender(sender);
        driversList = new LinkedList<>();
        chatStates = new HashMap<>();
        userAddress = new HashMap<>();
    }

    /**
     * Replies on /start command with two roles to choose from
     * @param chatId of the chat message comes from
     */
    public void replyToStart(long chatId) throws TelegramApiException{
        chatStates.put(chatId, State.CHOOSING_ROLE);
        menuSender.sendChooseRoleMenu(chatId);
    }

//    @SneakyThrows
    public void handleUpdate(Update upd) {
        try {
            long chatId = AbilityUtils.getChatId(upd);

            if (upd.hasMessage() && upd.getMessage().hasText() && upd.getMessage().getText().indexOf('/') != 0) {
                String message = upd.getMessage().getText();

                switch (chatStates.get(chatId)) {
                    case CHOOSING_ROLE:
                        if (message.equals(Constants.CHOOSE_ROLE_DRIVER)) {
                            replyToChooseRoleDriver(chatId);
                        } else if (message.equals(Constants.CHOOSE_ROLE_PASSENGER)) {
                            replyToChooseRolePassenger(chatId);
                            break;
                        } else {
                            menuSender.sendChooseRoleMenu(chatId);
                        }
                        break;
                    case DRIVER_ACTIVE:
                        if (message.equals(Constants.STOP_BROADCAST)) {
                            replyToStopBroadcast(chatId);
                        } else if (message.equals(Constants.CANCEL)) {
                            // TODO: refactor move to separate method
                            chatStates.put(chatId, State.CHOOSING_ROLE);
                            driversList.remove(chatId);
                            menuSender.sendChooseRoleMenu(chatId);
                        } else {
                            menuSender.sendDriverActiveMenu(chatId);
                        }
                        break;
                    case DRIVER_INACTIVE:
                        if (message.equals(Constants.RESUME_BROADCAST)) {
                            replyToChooseRoleDriver(chatId);
                        } else if (message.equals(Constants.CANCEL)) {
                            // TODO: refactor move to separate method
                            chatStates.put(chatId, State.CHOOSING_ROLE);
                            menuSender.sendChooseRoleMenu(chatId);
                        } else {
                            menuSender.sendDriverInactiveMenu(chatId);
                        }
                        break;
                    case CHOOSE_TRIP_TYPE:
                        switch (message) {
                            case Constants.CHOOSE_FROM_STATION:
                                replyToChooseFromStation(chatId);
                                break;
                            case Constants.CHOOSE_TO_STATION:
                                replyToChooseToStation(chatId);
                                break;
                            case Constants.CANCEL:
                                // TODO: refactor move to separate method
                                chatStates.put(chatId, State.CHOOSING_ROLE);
                                menuSender.sendChooseRoleMenu(chatId);
                                break;
                        }
                        break;
                    case ENTER_FROM_ADDRESS:
                        if (message.equals(Constants.CANCEL)) {
                            // TODO: refactor move to separate method
                            chatStates.put(chatId, State.CHOOSE_TRIP_TYPE);
                            menuSender.sendChooseTripTypeMenu(chatId);
                        }
                        else if (!message.isEmpty() && !message.isBlank()) {
                            replyToEnterFromAddress(chatId, upd);
                        } else {
                            menuSender.sendEnterFromAddressMenu(chatId);
                        }
                        break;
                    case ENTER_TO_ADDRESS:
                        if (message.equals(Constants.CANCEL)) {
                            // TODO: refactor move to separate method
                            chatStates.put(chatId, State.CHOOSE_TRIP_TYPE);
                            menuSender.sendChooseTripTypeMenu(chatId);
                        }
                        else if (!message.isEmpty() && !message.isBlank()) {
                            replyToEnterToAddress(chatId, upd);
                        } else {
                            menuSender.sendEnterToAddressMenu(chatId);
                        }
                        break;
                    case APPROVE_FROM_ADDRESS:
                        switch (message) {
                            case Constants.APPROVE_ADDRESS:
                                replyToApproveAddress(chatId, upd);
                                break;
                            case Constants.CANCEL:
                                // TODO: refactor move to separate method
                                chatStates.put(chatId, State.ENTER_FROM_ADDRESS);
                                menuSender.sendEnterToAddressMenu(chatId);
                                break;
                            default:
                                // TODO: refactor move to separate method
                                replyToEnterFromAddress(chatId, upd);
                        }
                        break;
                    case APPROVE_TO_ADDRESS:
                        switch (message) {
                            case Constants.APPROVE_ADDRESS:
                                replyToApproveAddress(chatId, upd);
                                break;
                            case Constants.CANCEL:
                                // TODO: refactor
                                chatStates.put(chatId, State.ENTER_TO_ADDRESS);
                                menuSender.sendEnterToAddressMenu(chatId);
                                break;
                            default:
                                // TODO: refactor
                                replyToEnterToAddress(chatId, upd);
                        }
                        break;
                    case LOOKING_FOR_DRIVER:
                        if (message.equals(Constants.CANCEL_TRIP)) {
                            // TODO: track trip request recipients to delete it from every chat
                            replyToCancelTrip(chatId);
                        } else {
                            replyWithText(chatId, Constants.REQUEST_PENDING_MESSAGE);
                        }
                }
            } else {
                SendMessage.builder().chatId(String.valueOf(chatId)).text("Пусте повідомлення :(");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void replyToChooseRoleDriver(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.DRIVER_ACTIVE);
        driversList.add(chatId);
        menuSender.sendDriverActiveMenu(chatId);
    }

    public void replyToChooseRolePassenger(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.CHOOSE_TRIP_TYPE);
        menuSender.sendChooseTripTypeMenu(chatId);
    }

    public void replyToStopBroadcast(long chatId) throws TelegramApiException{
        chatStates.put(chatId, State.DRIVER_INACTIVE);
        driversList.remove(chatId);
        menuSender.sendDriverInactiveMenu(chatId);
    }

    public void replyToChooseToStation(long chatId) throws TelegramApiException{
        chatStates.put(chatId, State.ENTER_FROM_ADDRESS);
        menuSender.sendEnterFromAddressMenu(chatId);
    }

    private void replyToChooseFromStation(long chatId) throws TelegramApiException {
        chatStates.put(chatId, State.ENTER_TO_ADDRESS);
        menuSender.sendEnterToAddressMenu(chatId);
    }

    private void replyToEnterFromAddress(long chatId, Update upd) throws TelegramApiException {
        String message = upd.getMessage().getText();
        userAddress.put(chatId, message);
        chatStates.put(chatId, State.APPROVE_TO_ADDRESS);
        User user = AbilityUtils.getUser(upd);
        // username may be absent
        String checkFromString = String.format("Перевірте запит:\n%s %s просить підвезти з вул. %s на вокзал\n%s", user.getFirstName(), user.getLastName(), message, user.getUserName());
        replyWithTextAndMenu(chatId, checkFromString, KeyboardFactory.approveAddressReplyKeyboard());
    }

    private void replyToEnterToAddress(long chatId, Update upd) throws TelegramApiException {
        String message = upd.getMessage().getText();
        User user = AbilityUtils.getUser(upd);
        userAddress.put(chatId, message);
        chatStates.put(chatId, State.APPROVE_TO_ADDRESS);
        // username may be absent
        String checkToString = String.format("Перевірте запит:\n%s %s просить підвезти з вокзалу на вул. %s\n%s", user.getFirstName(), user.getLastName(), message, user.getUserName());
        replyWithTextAndMenu(chatId, checkToString, KeyboardFactory.approveAddressReplyKeyboard());

    }

    public void replyToApproveAddress(long chatId, Update upd) throws TelegramApiException {
        chatStates.put(chatId, State.LOOKING_FOR_DRIVER);
        replyToFindCarButton(chatId, AbilityUtils.getUser(upd), userAddress.get(chatId));
    }

    public void replyToFindCarButton(long chatId, User user, String address) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(String.format("%s %s шукає транспорт на вул. %s \n%s",
                user.getFirstName(), user.getLastName(), address, user.getUserName()));

        for (Long driver : driversList) {
            sendMessage.setChatId(String.valueOf(driver));
            sender.execute(sendMessage);
        }

        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(Constants.REQUEST_SENT_MESSAGE);
        sendMessage.setReplyMarkup(KeyboardFactory.lookingForDriverReplyMenu());

        sender.execute(sendMessage);
    }

//    private void replyToCancelTrip(long chatId, User user, String address) throws TelegramApiException {
    private void replyToCancelTrip(long chatId) throws TelegramApiException {
//        DeleteMessage deleteMessage = new DeleteMessage();
//
//        for (Long driver : driversList) {
//            deleteMessage.setChatId(String.valueOf(driver));
//            deleteMessage.setMessageId(1);
//
//            sender.execute(deleteMessage);
//        }

        chatStates.put(chatId, State.CHOOSE_TRIP_TYPE);
        replyWithText(chatId, Constants.TRIP_CANCELED_SUCCESS_MESSAGE);

        menuSender.sendChooseTripTypeMenu(chatId);
    }

    private void replyWithText(long chatId, String messageText) throws TelegramApiException {
        sender.execute(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .build());
    }

    private void replyWithMenu(long chatId, ReplyKeyboardMarkup menu) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setText("");
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setReplyMarkup(menu);

        sender.execute(sendMessage);
    }

    private void replyWithTextAndMenu(long chatId, String messageText, ReplyKeyboardMarkup menu) throws TelegramApiException {
        sender.execute(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .replyMarkup(menu)
                .build());
    }
}
