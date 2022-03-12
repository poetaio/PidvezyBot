package bots;

import bots.response_handler.ResponseHandler;
import bots.utils.Constants;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class PidvesyBot extends AbilityBot {
    private final ResponseHandler responseHandler;

    @Override
    public long creatorId() {
        return Constants.CREATOR_ID;
    }
    
    public PidvesyBot() {
        super(Constants.BOT_TOKEN, Constants.BOT_USERNAME);
        responseHandler = new ResponseHandler(sender);
    }

    public Ability start() {
        return Ability
                .builder()
                .name("start")
                .info(Constants.START_DESCRIPTION)
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    try {
                        responseHandler.replyToStart(ctx.chatId());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    @Override
    public void onUpdateReceived(Update update) {
        super.onUpdateReceived(update);
        try {
            responseHandler.handleUpdate(update);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
