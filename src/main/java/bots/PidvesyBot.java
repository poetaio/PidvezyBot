package bots;

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
        // TODO: REMOVE
        responseHandler = ResponseHandler.getInstance(sender);

//        setupTrainSchedule();
    }

//    private void setupTrainSchedule() {
//        String date_string3 = "2022-03-14-13:00";
//        String date_string4 = "2022-03-14-15:30";
//        String date_string5 = "2022-03-14-16:00";
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm");
//        Date date3 = null;
//        Date date4 = null;
//        Date date5 = null;
//        try {
//            date3 = formatter.parse(date_string3);
//            date4 = formatter.parse(date_string4);
//            date5 = formatter.parse(date_string5);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//
//        String trainMessage = "Потяг має прибути о 16:00";
//        new Thread(new BroadcastScheduler(trainMessage, trainMessage, date3)).start();
//        new Thread(new BroadcastScheduler("Потяг прибуває через 30 хвилин", trainMessage, date4)).start();
//        new Thread(new BroadcastScheduler("Потяг прибув!", "Потяг прибув!", date5)).start();
//    }

    public Ability start() {
        return Ability
                .builder()
                .name("start")
                .info(Constants.START_DESCRIPTION)
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    try {
                        responseHandler.replyToStart(ctx.chatId(), ctx.update());
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
