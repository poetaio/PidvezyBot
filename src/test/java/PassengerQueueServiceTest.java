import models.dao.QueuePassengerDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import services.PassengerQueueService;

public class PassengerQueueServiceTest {
    private static PassengerQueueService passengerQueueService;
    @BeforeAll
    static void setup() {
        passengerQueueService = PassengerQueueService.getInstance();
    }

    @BeforeEach
    void removeAllPassengers() {
        passengerQueueService.removeAll();
    }

    @Test
    void shouldGetNextTrip() {
        passengerQueueService.add(new QueuePassengerDao(1, "Address", "Details"));
    }
}
