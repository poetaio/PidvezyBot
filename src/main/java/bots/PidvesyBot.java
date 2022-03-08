package bots;

import bots.utils.Constants;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
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

//    public Ability back() {
//        System.out.println("Here");
//        return Ability
//                .builder()
//                .name("Назад")
//                .info(Constants.START_DESCRIPTION)
//                .locality(Locality.ALL)
//                .privacy(Privacy.PUBLIC)
//                .action(ctx -> responseHandler.replyToStart(ctx.chatId()))
//                .build();
//    }

//    public Reply replyChooseRoleButtons() {
//        return Reply.of(action, Flag.CALLBACK_QUERY);
//    }

    @Override
    public void onUpdateReceived(Update update) {
        super.onUpdateReceived(update);
        responseHandler.handleUpdate(update);
    }

//    public Reply replyToFindCarButton() {
//        System.out.println("find");
//        Consumer<Update> action = upd -> responseHandler.replyToFindCarButton(AbilityUtils.getChatId(upd), AbilityUtils.getUser(upd));
//        return Reply.of(action, Flag.CALLBACK_QUERY);
//    }
}
