package bots;

import bots.response_handler.ResponseHandler;
import bots.utils.Constants;
import models.dao.QueuePassengerDao;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

//        String date_string1 = "12:52";
//        String date_string2 = "12:51";
//        String date_string3 = "2022-03-14-12:54";
        String date_string3 = "2022-03-14-13:00";
        String date_string4 = "2022-03-14-15:30";
        String date_string5 = "2022-03-14-16:00";
        //Instantiating the SimpleDateFormat class
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm");
        //Parsing the given String to Date object
//        Date date1 = null;
//        Date date2 = null;
        Date date3 = null;
        Date date4 = null;
        Date date5 = null;
        try {
//            date1 = formatter.parse(date_string1);
//            date2 = formatter.parse(date_string2);
            date3 = formatter.parse(date_string3);
            date4 = formatter.parse(date_string4);
            date5 = formatter.parse(date_string5);
        } catch (ParseException e) {
            e.printStackTrace();
        }

//        new Thread(new BroadcastScheduler("Повідомлення для водіїв 1", date1)).start();
//        new Thread(new BroadcastScheduler("Повідомлення для водіїв 2 ", date2)).start();
        String trainMessage = "Потяг має прибути о 16:00";
        new Thread(new BroadcastScheduler(trainMessage, trainMessage, date3)).start();
        new Thread(new BroadcastScheduler("Потяг прибуває через 30 хвилин", trainMessage, date4)).start();
        new Thread(new BroadcastScheduler("Потяг прибув!", "Потяг прибув!", date5)).start();
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
