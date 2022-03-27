package bots.pidvezy_group_bot;

import bots.utils.Constants;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class PidvezyGroupBot extends AbilityBot {
    private final UpdateHandler updateHandler;

    @Override
    public long creatorId() {
        return Constants.CREATOR_ID;
    }

    public PidvezyGroupBot() {
        super(System.getenv("GROUP_BOT_TOKEN"), Constants.GROUP_BOT_USERNAME);

        try {
            updateHandler = new UpdateHandler(sender, getMe().getId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create bot!");
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        super.onUpdateReceived(update);

        try {
            // if message's not empty handle update
            if (update.hasMessage())
                updateHandler.handleUpdate(update);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
