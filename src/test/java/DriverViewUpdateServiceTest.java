import bots.utils.Constants;
import models.dao.DriverUpdateDao;
import org.junit.jupiter.api.*;
import services.driver_services.DriverViewUpdateService;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DriverViewUpdateServiceTest {
    private static DriverViewUpdateService driverViewUpdateService;

    @BeforeAll
    static void setup() {
        driverViewUpdateService = DriverViewUpdateService.getInstance();
    }

    @BeforeEach
    void removeAllDaos() {
        driverViewUpdateService.removeAll();
    }

    @RepeatedTest(20)
    void ShouldInsertDriversWithCorrectDate() {
        long chatId = 1;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, Constants.DRIVER_UPDATE_INTERVAL);
        long expectedTime = calendar.getTimeInMillis();

        driverViewUpdateService.addDriver(chatId);
        Date actualTime = driverViewUpdateService.getDriver(chatId).getNextUpdateTime();

        Assertions.assertTrue(actualTime.getTime() - expectedTime < 200);
    }

    @Test
    void shouldRescheduleDriverUpdateDaoAndMoveToTheEndOfQueue() throws InterruptedException {
        long chatId1 = 1;
        long chatId2 = 2;

        driverViewUpdateService.addDriver(chatId1);
        driverViewUpdateService.addDriver(chatId2);
        Thread.sleep(1000);
        driverViewUpdateService.resetDriverTime(chatId1);

        List<DriverUpdateDao> resList = driverViewUpdateService.getAll();
        long expectedFirst = 2;
        long expectedSecond = 1;
        Assertions.assertEquals(resList.get(0).getChatId(), expectedFirst);
        Assertions.assertEquals(resList.get(1).getChatId(), expectedSecond);
    }
}
