import bots.utils.Constants;
import models.dao.DriverUpdateDao;
import org.junit.jupiter.api.*;
import services.DriverUpdateService;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DriverUpdateServiceTest {
    private static DriverUpdateService driverUpdateService;

    @BeforeAll
    static void setup() {
        driverUpdateService = DriverUpdateService.getInstance();
    }

    @BeforeEach
    void removeAllDaos() {
        driverUpdateService.removeAll();
    }

    @RepeatedTest(20)
    void ShouldInsertDriversWithCorrectDate() {
        long chatId = 1;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);
        long expectedTime = calendar.getTimeInMillis();

        driverUpdateService.addDriver(chatId);
        Date actualTime = driverUpdateService.getDriver(chatId).getNextUpdateTime();

        Assertions.assertTrue(actualTime.getTime() - expectedTime < 200);
    }

    @Test
    void shouldRescheduleDriverUpdateDaoAndMoveToTheEndOfQueue() throws InterruptedException {
        long chatId1 = 1;
        long chatId2 = 2;

        driverUpdateService.addDriver(chatId1);
        driverUpdateService.addDriver(chatId2);
        Thread.sleep(1000);
        driverUpdateService.resetDriverTime(chatId1);

        List<DriverUpdateDao> resList = driverUpdateService.getAll();
        long expectedFirst = 2;
        long expectedSecond = 1;
        Assertions.assertEquals(resList.get(0).getChatId(), expectedFirst);
        Assertions.assertEquals(resList.get(1).getChatId(), expectedSecond);
    }
}
