import models.QueueTrip;
import services.trip_services.utils.TripComparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.trip_services.TripQueueService;

import java.util.PriorityQueue;
import java.util.Queue;

public class PassengerQueueServiceTest {
    private static TripQueueService passengerQueueService;
    @BeforeAll
    static void setup() {
        passengerQueueService = TripQueueService.getInstance();
    }

    @BeforeEach
    void removeAllPassengers() {
        passengerQueueService.removeAll();
    }

    @Test
    void shouldGetNextTrip() {
        passengerQueueService.add(new QueueTrip(1, "Address", "Details"));
    }

    @Test
    void shouldInsertInTheRightOrder() throws InterruptedException {
        // setup
        Queue<QueueTrip> tripQueue = new PriorityQueue<>(TripComparator.TRIP_COMPARATOR);
        QueueTrip firstTrip = new QueueTrip(1, "address", "details");
        QueueTrip secondTrip = new QueueTrip(2, "address", "details");
        QueueTrip thirdTrip = new QueueTrip(3, "address", "details");

        //action
        firstTrip.addDriverChatId(1);
        secondTrip.addDriverChatId(2);
        thirdTrip.addDriverChatId(3);

        Thread.sleep(1000);
        secondTrip.addDriverChatId(4);

        Thread.sleep(1000);
        thirdTrip.addDriverChatId(5);

        tripQueue.add(thirdTrip);
        tripQueue.add(secondTrip);
        tripQueue.add(firstTrip);

        // assert
        Assertions.assertEquals(firstTrip, tripQueue.poll());
        Assertions.assertEquals(secondTrip, tripQueue.poll());
        Assertions.assertEquals(thirdTrip, tripQueue.poll());
    }
}
