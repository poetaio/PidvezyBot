package bots;

import bots.utils.Constants;
import bots.utils.State;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
    public void replyToStart(long chatId) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.enableMarkdown(true);
            sendMessage.setText(Constants.CHOOSE_ROLE_REPLY);
            sendMessage.setChatId(String.valueOf(chatId));
            // send menu with two roles
            sendMessage.setReplyMarkup(KeyboardFactory.chooseRoleReplyKeyboard());
            chatStates.put(chatId, State.CHOOSING_ROLE);

            sender.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

//    @SneakyThrows
    public void handleUpdate(Update upd) {
        try {
            long chatId = AbilityUtils.getChatId(upd);

            if (upd.hasMessage() && upd.getMessage().hasText()) {
                String message = upd.getMessage().getText();
                User user;

                // TODO: refactor       !!! CRITICAL
                if (chatStates.get(chatId) == State.ENTER_FROM_ADDRESS) {
                    userAddress.put(chatId, message);
                    chatStates.put(chatId, State.APPROVE_ADDRESS);
                    user = AbilityUtils.getUser(upd);
                    // username may be absent
                    String checkFromString = String.format("Перевірте запит:\n%s %s просить підвезти з вул. %s на вокзал\n%s", user.getFirstName(), user.getLastName(), message, user.getUserName());
                    replyWithTextAndMenu(chatId, checkFromString, KeyboardFactory.approveAddressReplyKeyboard());
                } else if (chatStates.get(chatId) == State.ENTER_TO_ADDRESS) {
                    userAddress.put(chatId, message);
                    chatStates.put(chatId, State.APPROVE_ADDRESS);
                    user = AbilityUtils.getUser(upd);
                    // username may be absent
                    String checkToString = String.format("Перевірте запит:\n%s %s просить підвезти з вокзалу на вул. %s\n%s", user.getFirstName(), user.getLastName(), message, user.getUserName());
                    replyWithTextAndMenu(chatId, checkToString, KeyboardFactory.approveAddressReplyKeyboard());
                } else {
                    switch (message) {
//                case Constants.CHOOSE_ROLE_REPLY:
//                    replyToChooseRoleButtons(chatId, message);
//                    break;
                        case Constants.CHOOSE_ROLE_DRIVER:
                        case Constants.RESUME_BROADCAST:
                            chatStates.put(chatId, State.STOP_BROADCAST);
//                    replyToChoseDriver(chatId);
                            driversList.add(chatId);
//                        menuSender.sendStopBroadcastMenu(chatId);
                            replyWithTextAndMenu(chatId, "Сєва, придумай тєкст, пліз, без нього нот воркінг (1)", KeyboardFactory.stopBroadcastReplyMarkup());
                            break;
                        case Constants.STOP_BROADCAST:
                            chatStates.put(chatId, State.RESUME_BROADCAST);
                            driversList.remove(chatId);
//                        menuSender.sendResumeBroadcastMenu(chatId);
                            replyWithTextAndMenu(chatId, Constants.BROADCAST_STOPPED_TEXT, KeyboardFactory.resumeBroadcastReplyMarkup());
                            break;
                        case Constants.CHOOSE_ROLE_PASSENGER:
                            chatStates.put(chatId, State.CHOOSE_FROM_OR_TO);
//                        replyToChosePassenger(chatId);
                            replyWithTextAndMenu(chatId, "Сєва, придумай тєкст, пліз, без нього нот воркінг (2)", KeyboardFactory.chooseDestinationTypeReplyKeyboard());
                            break;
                        case Constants.CHOOSE_FROM_STATION:
                            chatStates.put(chatId, State.ENTER_TO_ADDRESS);
                            replyWithTextAndMenu(chatId, Constants.ENTER_TO_ADDRESS, KeyboardFactory.enterToAddressReplyKeyboard());
                            break;
                        case Constants.CHOOSE_TO_STATION:
                            chatStates.put(chatId, State.ENTER_FROM_ADDRESS);
//                        menuSender.sendEnterFromAddress(chatId);
                            replyWithTextAndMenu(chatId, Constants.ENTER_FROM_ADDRESS, KeyboardFactory.enterFromAddressReplyKeyboard());
                            break;
//                        case Constants.ENTER_FROM_ADDRESS:
////                            chatStates.put(chatId, State.APPROVE_ADDRESS);
////                            user = AbilityUtils.getUser(upd);
////                            // username may be absent
////                            String checkFromString = String.format("Перевірте запит:\n%s %s просить підвезти з вул. %s на вокзал\n%s", user.getFirstName(), user.getLastName(), message, user.getUserName());
////                            replyWithTextAndMenu(chatId, checkFromString, KeyboardFactory.approveAddressReplyKeyboard());
//                            break;
//                        case Constants.ENTER_TO_ADDRESS:
//                            chatStates.put(chatId, State.APPROVE_ADDRESS);
//                            user = AbilityUtils.getUser(upd);
//                            // username may be absent
//                            String checkToString = String.format("Перевірте запит:\n%s %s просить підвезти з вокзалу на вул. %s\n%s", user.getFirstName(), user.getLastName(), message, user.getUserName());
//                            replyWithTextAndMenu(chatId, checkToString, KeyboardFactory.approveAddressReplyKeyboard());
//                            break;
                        case Constants.APPROVE_ADDRESS:
                            chatStates.put(chatId, State.LOOKING_FOR_DRIVER);
                            replyToFindCarButton(chatId, AbilityUtils.getUser(upd), userAddress.get(chatId));
                            break;
                        case Constants.CANCEL:
                            replyToCancel(chatId);
                            break;
                        default:
//                        SendMessage.builder().chatId(String.valueOf(chatId)).text("Невідома команда :(");
                    }
                }
            } else {
                System.out.println("No text");
                SendMessage.builder().chatId(String.valueOf(chatId)).text("Пусте повідомлення :(");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replyToCancel(long chatId) {
        switch (chatStates.get(chatId)) {
            case CHOOSING_ROLE:

                break;
            case STOP_BROADCAST:
                break;
            case RESUME_BROADCAST:
                break;
            case CHOOSE_FROM_OR_TO:
                break;
            case ENTER_FROM_ADDRESS:
                break;
            case APPROVE_ADDRESS:
                break;
            case LOOKING_FOR_DRIVER:
                break;
        }
    }

//    public void replyToChooseRoleButtons(long chatId, String buttonId) {
//        try {
//            switch (buttonId) {
//                case Constants.ROLE_DRIVER:
//                    replyToChoseDriver(chatId);
//                    break;
//                case Constants.ROLE_PASSENGER:
//                    replyToChosePassenger(chatId);
//                    break;
//            }
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }

//    private void replyToChoseDriver(long chatId) throws TelegramApiException {
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.setText("You chose driver");
//        sendMessage.setChatId(String.valueOf(chatId));
//
//        driversList.add(chatId);
//
//        sender.execute(sendMessage);
//    }

//    private void replyToChosePassenger(long chatId) throws TelegramApiException {
//    }

    public void replyToFindCarButton(long chatId, User user, String address) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(String.format("%s %s шукає транспорт на вул. %s \n%s",
                    user.getFirstName(), user.getLastName(), address, user.getUserName()));

            for (Long driver : driversList) {
                sendMessage.setChatId(String.valueOf(driver));
                sender.execute(sendMessage);
            }

            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(Constants.REQUEST_SENT_MESSAGE);
            sender.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
}
