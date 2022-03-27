package bots.pidvezy_group_bot.trip_update_handler.new_trip;

import models.dao.SendTripDao;

public interface OnNewTrip {
    void onNewTrip(SendTripDao sendTrip);
}
