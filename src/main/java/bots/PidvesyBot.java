package bots;

import bots.utils.Constants;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Consumer;

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
                .action(ctx -> responseHandler.replyToStart(ctx.chatId()))
                .build();
    }

    public Reply replyToStartButtons() {
        Consumer<Update> action = upd -> responseHandler.replyToChooseRoleButtons(AbilityUtils.getChatId(upd), upd.getCallbackQuery().getData());
        return Reply.of(action, Flag.CALLBACK_QUERY);
    }

    public Reply replyToFindCarButton() {
        Consumer<Update> action = upd -> responseHandler.replyToFindCarButton(AbilityUtils.getChatId(upd), AbilityUtils.getUser(upd));
        return Reply.of(action, Flag.CALLBACK_QUERY);
    }
}
