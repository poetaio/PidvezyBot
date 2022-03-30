package services;

import bot.utils.Constants;

import java.util.Calendar;
import java.util.TimeZone;

public class CurfewService {
    public static boolean isNowCurfew() {
        int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
        if (Constants.CURFEW_START_HOUR > Constants.CURFEW_END_HOUR) {
            // if curfew starts today and ends tomorrow (22:00 - 6:00)
            return currentHour >= Constants.CURFEW_START_HOUR || currentHour < Constants.CURFEW_END_HOUR;
        }

        // if curfew starts today and ends today (18:00 - 23:00)
        return currentHour >= Constants.CURFEW_START_HOUR && currentHour < Constants.CURFEW_END_HOUR;
    }
}
