import models.QueueTrip;
import models.utils.TripComparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

public class TripComparatorTest {
    @Test
    void shouldCompareRight() throws InterruptedException {
        Comparator<QueueTrip> tripComparator = TripComparator.TRIP_COMPARATOR;

        QueueTrip firstTrip = new QueueTrip(1, "address", "details");
        QueueTrip secondTrip = new QueueTrip(2, "address", "details");
        QueueTrip thirdTrip = new QueueTrip(3, "address", "details");

        firstTrip.addDriverChatId(1);
        secondTrip.addDriverChatId(2);
        thirdTrip.addDriverChatId(3);
        Thread.sleep(1000);
        secondTrip.addDriverChatId(4);
        Thread.sleep(2000);
        thirdTrip.addDriverChatId(5);

        Assertions.assertEquals(1, tripComparator.compare(firstTrip, secondTrip));
        Assertions.assertEquals(1, tripComparator.compare(firstTrip, thirdTrip));
        Assertions.assertEquals(1, tripComparator.compare(secondTrip, thirdTrip));
    }
}
