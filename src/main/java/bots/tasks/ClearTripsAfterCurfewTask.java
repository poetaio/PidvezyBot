package bots.tasks;

import bots.tasks.utils.ClearCallback;
import bots.utils.Constants;

import java.util.Calendar;
import java.util.TimeZone;

public class ClearTripsAfterCurfewTask implements Runnable {
    private final ClearCallback clearCallback;
    private boolean wasCurfew;
    private int INTERVAL_5_MIN = 300000;
    private int INTERVAL_10_MIN = 600000;
    private int INTERVAL_1_HOUR = 3600000;

    public ClearTripsAfterCurfewTask(ClearCallback clearCallback) {
        this.clearCallback = clearCallback;
        // to perform clean up on every start
        wasCurfew = true;
    }

    private boolean isNowCurfew() {
        int currentHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+2")).get(Calendar.HOUR_OF_DAY);
        return currentHour >= Constants.CURFEW_START_HOUR || currentHour < Constants.CURFEW_END_HOUR;
    }

    @Override
    public void run() {
        boolean nowCurfew = isNowCurfew();
        int interval = nowCurfew ? INTERVAL_5_MIN : INTERVAL_10_MIN;
        while (true) {
            nowCurfew = isNowCurfew();
            if (wasCurfew && !nowCurfew) {
                // if passenger is looking for a trip
                // set try_again_during_curfew
                clearCallback.clear();
                
                // 10 min
                interval = INTERVAL_10_MIN;
            } else if (!wasCurfew && nowCurfew){
                // 5 min
                interval = INTERVAL_5_MIN;
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
