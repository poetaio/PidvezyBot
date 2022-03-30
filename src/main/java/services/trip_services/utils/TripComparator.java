package services.trip_services.utils;

import models.QueueTrip;

import java.util.Comparator;
import java.util.Date;

public class TripComparator {
    public static final Comparator<Date> NULL_DATE_COMPARATOR = (o1, o2) -> {
        if (o1 == null)
            return -1;
        if (o2 == null)
            return 1;
        return o1.compareTo(o2);
    };

    public final static Comparator<QueueTrip> TRIP_COMPARATOR =
            Comparator.comparing(QueueTrip::getDriverCount)
                    .thenComparing(QueueTrip::getLastViewDate, NULL_DATE_COMPARATOR);
}
