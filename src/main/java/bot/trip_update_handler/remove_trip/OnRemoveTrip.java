package bot.trip_update_handler.remove_trip;

import models.dao.SendTripDao;

public interface OnRemoveTrip {
    void onRemoveTrip(SendTripDao sendTripDao);
}
