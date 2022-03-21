package bots.tasks;

import bots.tasks.utils.ClearCallback;
import bots.utils.Constants;

import java.util.Calendar;
import java.util.TimeZone;

public class ClearTripsAfterCurfewTask implements Runnable {
    private final ClearCallback clearCallback;
    private boolean wasCurfew;
    private int interval;

    public ClearTripsAfterCurfewTask(ClearCallback clearCallback) {
        this.clearCallback = clearCallback;
        // to perform clean up on every start
        wasCurfew = true;
    }

    private boolean isNowCurfew() {
        int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
        return currentHour >= Constants.CURFEW_END_HOUR || currentHour <= Constants.CURFEW_START_HOUR;
    }

    @Override
    public void run() {
        boolean nowCurfew;
        while (true) {
            nowCurfew = isNowCurfew();
            if (wasCurfew && !nowCurfew) {
                // if passenger is looking for a trip
                // set try_again_during_curfew
                clearCallback.clear();
                
                // 1 hour
                interval = 3600000;
            } else if (!wasCurfew && nowCurfew){
                // 5 min
//                interval = 30000;
                // 10s
                interval = 10000;
            }
            wasCurfew = nowCurfew;

            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
