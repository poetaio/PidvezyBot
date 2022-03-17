package bots.utils;

public interface Constants {
    String BOT_USERNAME = "PidvezyBot";
    int CREATOR_ID = 278171783;
    int DRIVER_UPDATE_INTERVAL = 5;
    int CURFEW_START_HOUR = 18;
    int CURFEW_END_HOUR = 5;

    String DRIVER_STATES = "DRIVER_STATES";
    String PASSENGER_STATES = "PASSENGER_STATES";

    String START_DESCRIPTION = String.format("Бот для допомоги з розвезенням біженців із залізничного вокзалу у Львові під час комендантської години (%s:00 - %s:00)", CURFEW_START_HOUR, CURFEW_END_HOUR);

//    String START_REPLY = "З якою ціллю ви тут?";
    String CHOOSE_ROLE_REPLY = String.format("Бот для допомоги з розвезенням біженців із залізничного вокзалу у Львові під час комендантської години (%s:00 - %s:00)", CURFEW_START_HOUR, CURFEW_END_HOUR);

//    String ROLE_DRIVER = "Підвезу";
//    String ROLE_PASSENGER = "Підвезіть";

    String CHOOSE_ROLE_DRIVER = "Я волонтер";

    String TAKE_TRIP = "Відгукнутися";
    String TRIP_TAKEN_MESSAGE = "Поїздка уже неактуальна :(";

    String NEXT_TRIP = "Наступний запит";

    String STOP_BROADCAST = "Зупинити розсилку";
    String BROADCAST_STOPPED_TEXT = "Розсилку зупинено";
//    String NO_TRIPS_MESSAGE = "На даний момент немає запитів. Очікуйте на оновлення.";
    String IS_LOOKING_FOR_CAR_MESSAGE = "%s%s шукає транспорт з вокзалу на <b>%s</b>.\n\n<b>%s</b>";

    String IS_WAITING_FOR_A_CALL_MESSAGE = "%s чекає на ваше повідомлення або дзвінок\n@%s\n%s\n%s\n%s";
    String NO_TRIPS_MESSAGE = "Наразі запитів немає. Ми вам повідомимо про їхню наявність та про прибуття потягу";

    String DRIVER_PIDVEZY = "Підвезу";
    String DRIVER_DISMISS_TRIP = "Відмовитись";

    String RESUME_BROADCAST = "Відновити розсилку";

    String CHOOSE_ROLE_PASSENGER = "Я потребую транспорт";

    String ENTER_ADDRESS = "Введіть кінцеву адресу прибуття:\nЗ вокзалу на ...";
    String ENTER_DETAILS = "Вкажіть інформацію про пасажирів. Наприклад, \"троє людей і два коти\" або \"одна людина з великою валізою\"";
    String ENTER_NUMBER_MESSAGE = "Вкажіть ваш контактний номер телефону";

    String ENTER_NUMBER = "Вказати номер телефону";

    String EDIT_ADDRESS = "Введіть кінцеву адресу прибуття:\nЗ вокзалу на <b>%s</b>";
    String EDIT_DETAILS = "Вкажіть інформацію про пасажирів. Наприклад, \"троє людей і два коти\" або \"одна людина з великою валізою\"\n<b>%s</b>";

    String DO_NOT_CHANGE = "Лишити без змін";

    String ARE_YOU_ON_STATION = "Ви вже прибули на станцію?";
    String ON_STATION_NO = "Ні";
    String ON_STATION_YES = "Так";

    String I_AM_ON_STATION = "Я прибув на вокзал";
    String CHECKING_OUT_ON_STATION_MESSAGE = "Волонтери завчасно отримують інформацію про прибуття потягу, щоб чекати вас на місці.";

    String CHANGE_TRIP_INFO = "Змінити запит";
    String APPROVE_TRIP = "Підтвердити";
    String TRY_AGAIN = "Спробувати ще раз";

    String APPROVE_MESSAGE_CURFEW = "Ваш запит збережено: \n\n%s%s шукає транспорт з вокзалу на %s.\n%s\n@%s\n%s\n\nБот працює лише під час комендантської години. Ви зможете його відправити після "+CURFEW_START_HOUR+":00";
    String APPROVE_MESSAGE = "Підтвердіть запит:\n\n%s%s шукає транспорт з вокзалу на %s.\n%s\n@%s\n%s";

    String REQUEST_SENT_MESSAGE = "Запит надіслано.\nЧекайте на сповіщення.\nПовідомте нам, коли знайдете транспорт.";
    String REQUEST_PENDING_MESSAGE = "В пошуках волонтерів\uD83D\uDD0E";

    String FOUND_TRIP = "Підтвердити поїздку з цим водієм";
    String FIND_AGAIN = "Шукати знову";

    String THANKS = "Дякую";

    String APPROVING_FINISH_TRIP = "Повідомте нас про завершення поїздки";
    String FINISH_TRIP = "Поїздка завершена";

    String EDIT_TRIP = "Змінити поїздку";
//    String TRIP_CANCELED_SUCCESS_MESSAGE = "Поїздку успішно скасовано";
    String I_FOUND_A_CAR = "Я знайшов транспорт";
    String STOP_LOOKING_FOR_A_CAR = "Зупинити пошук";

    String HAVE_A_NICE_TRIP = "Гарної дороги!";

    String BACK = "Назад";;

    String UNKNOWN_STATE_ERROR_MESSAGE = "Невідомий стан :( Зверніться в тех підтримку";
}
