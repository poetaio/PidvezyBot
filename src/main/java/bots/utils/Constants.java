package bots.utils;

public interface Constants {
    String BOT_USERNAME = "PidvezyBot";
    String ADMIN_SECRET = System.getenv("ADMIN_SECRET");
    int CREATOR_ID = 278171783;
    int DRIVER_UPDATE_INTERVAL = Integer.parseInt(System.getenv("DRIVER_UPDATE_INTERVAL"));
    int CURFEW_START_HOUR = Integer.parseInt(System.getenv("CURFEW_START_HOUR"));
    int CURFEW_END_HOUR = Integer.parseInt(System.getenv("CURFEW_END_HOUR"));

    String DRIVER_STATES = "DRIVER_STATES";
    String PASSENGER_STATES = "PASSENGER_STATES";

    String START_DESCRIPTION = String.format("Бот для допомоги з розвезенням людей із залізничного вокзалу у Львові під час комендантської години \\(%s:00 \\- %s:00\\)", CURFEW_START_HOUR, CURFEW_END_HOUR);

    String CHOOSE_ROLE_REPLY = String.format("Бот для допомоги з розвезенням людей із залізничного вокзалу у Львові під час комендантської години \\(%s:00 \\- %s:00\\)", CURFEW_START_HOUR, CURFEW_END_HOUR);

    String FAQ = "Як це працює?";
    String FAQ_MESSAGE = "Людина, що приїзжає вночі на вокзал потребує транспорт, щоб дістатися до кінцевого пункту прибуття\\.\n\nВона створює заявку, вказуючи адресу й інших пасажирів\\.\n\nАвтоволонтери, які користуються ботом, отримують сповіщення з заявками й можуть на них відгукнутися\\.\n\nАвтоволонтер й автор заявки отримують контакти один одного й домовляються про поїздку\\.";

    String CHOOSE_ROLE_DRIVER = "Я автоволонтер";

    String TAKE_TRIP = "Відгукнутися";
    String TRIP_TAKEN_MESSAGE = "Поїздка вже неактуальна :\\(";

    String NEXT_TRIP = "Наступний запит";

    String STOP_BROADCAST = "Зупинити розсилку";
    String BROADCAST_STOPPED_TEXT = "Розсилку зупинено";
//    String NO_TRIPS_MESSAGE = "На даний момент немає запитів\\. Очікуйте на оновлення\\.";
    String IS_LOOKING_FOR_CAR_MESSAGE = "%s%s шукає транспорт з вокзалу на *%s*\\.\n\n*%s*\n\n\\(Ви отримаєте новий запит через " + DRIVER_UPDATE_INTERVAL + "с\\)";

    String PASSENGER_TRIP_WAS_TAKEN = "%s відгукнувся на вашу заявку\n\n%s\n%s\\.\nДомовтеся з волонтером про зустріч\\.";

    String INFORM_US_TRIP_STATUS = "Повідомте нас про статус заявки";

    String IS_WAITING_FOR_A_CALL_MESSAGE = "%s чекає на ваше повідомлення або дзвінок\n%s\n%s\n\n%s\n%s";
    String NO_TRIPS_MESSAGE = "Наразі запитів немає\\. Ми вам повідомимо про їхню наявність\\.";

    String DRIVER_PIDVEZY = "Підвезу";
    String DRIVER_DISMISS_TRIP = "Скасувати поїздку";

    String GOOD_BOY_MESSAGE = "Ви молодець\\!";

    String RESUME_BROADCAST = "Відновити розсилку";

    String CHOOSE_ROLE_PASSENGER = "Я потребую транспорт";

    String ENTER_ADDRESS = "Введіть кінцеву адресу прибуття:\nЗ вокзалу на \\.\\.\\.";
    String ENTER_DETAILS = "Вкажіть інформацію про пасажирів\\. Наприклад, \"троє людей і два коти\" або \"одна людина з великою валізою\"";
    String PASSENGER_ENTER_NUMBER_MESSAGE = "Автоволонтерам потрібні ваші контакти\\. Будь ласка, вкажіть ваш номер телефону";
    String DRIVER_ENTER_NUMBER_MESSAGE = "Пасажирам потрібні ваші контакти\\. Будь ласка, вкажіть ваш номер телефону";

    String SHARE_NUMBER = "Поділитися номером телефону";

    String EDIT_ADDRESS_MESSAGE = "Введіть нову адресу прибуття\nАбо залиште минулу адресу без змін:\nЗ вокзалу на *%s*";
    String EDIT_DETAILS_MESSAGE = "Введіть нову інформацію про пасажирів\nАбо залиште минулі дані без змін:\n*%s*";

    String DO_NOT_CHANGE = "Лишити без змін";

    String ARE_YOU_ON_STATION = "Ви вже прибули на станцію?";
    String ON_STATION_NO = "Ні";
    String ON_STATION_YES = "Так";

    String I_AM_ON_STATION = "Я прибув на вокзал";
    String CHECKING_OUT_ON_STATION_MESSAGE = "Волонтери завчасно отримують інформацію про прибуття потягу, щоб чекати вас на місці\\.";

    String CHANGE_TRIP_INFO = "Змінити запит";
    String EDIT_ADDRESS = "Змінити адресу";
    String EDIT_DETAILS = "Змінити інформацію про пасажирів";
    String APPROVE_TRIP = "Відправити запит";
    String TRY_AGAIN = "Спробувати ще раз";
    String CURFEW_IS_OVER_MESSAGE = "Комендантська година завершена\\. Спробуйте пізніше\\.";

    String APPROVE_MESSAGE_CURFEW = "Ваш запит збережено: \n\n%s%s шукає транспорт з вокзалу на %s\\.\n%s\n%s\n\n%s\n\nБот працює лише під час комендантської години\\. Ви зможете його відправити після "+CURFEW_START_HOUR+":00";
    String APPROVE_MESSAGE = "Перевірте запит і натисніть \"Відправити запит\", щоб волонтери його отримали:\n\n%s%s шукає транспорт з вокзалу на %s\\.\n%s\n\n%s\n%s";

    String REQUEST_SENT_EXTENDED_MESSAGE = "Запит надіслано\\.\nЧекайте на сповіщення\\.\nПовідомте нам, коли знайдете транспорт\\.";
    String REQUEST_SENT_MESSAGE = "Запит надіслано\\.";
    String REQUEST_PENDING_MESSAGE = "В пошуках волонтерів\uD83D\uDD0E";
    String DRIVERS_GOT_YOUR_MESSAGE = "Волонтери отримали ваш запит\\. Очікуємо на підтвердження\\.\\.\\.";

    String CANCEL_TRIP = "Скасувати запит";

    String DRIVER_TOOK_YOUR_TRIP_MESSAGE = "%s відгукнувся на вашу заявку\n@%s";
    String LET_US_KNOW_ABOUT_TRIP_STATUS = "Повідомте нас про статус заявки";

//    String FOUND_TRIP = "Підтвердити поїздку з цим водієм";
    String FOUND_TRIP = "Завершити";
    String FIND_AGAIN = "Шукати знову";

    String THANKS = "Дякую";

    String LET_US_KNOW_WHEN_TRIP_IS_OVER = "Повідомте нас про завершення поїздки";
    String FINISH_TRIP = "Поїздка завершена";
    String AM_GOOD_BOY = "Я молодець!";

    String EDIT_TRIP = "Змінити поїздку";
    //    String TRIP_CANCELED_SUCCESS_MESSAGE = "Поїздку успішно скасовано";
    String I_FOUND_A_CAR = "Я знайшов транспорт";
    String STOP_LOOKING_FOR_A_CAR = "Зупинити пошук";
    String RESUME_SEARCH = "Відновити пошук";

    String HAVE_A_NICE_TRIP = "Гарної дороги\\!";

    String START_SEARCHING_AGAIN = "Повертаємось до пошуків";

    String BACK = "Назад";

    String GOOD_BOY = "Ви молодець\\!";

    String UNKNOWN_STATE_ERROR_MESSAGE = "Невідомий стан :\\( Зверніться в тех підтримку";
    String SEARCH_STOPPED_MESSAGE = "Запит зупинено:\n\n%s%s шукає траспорт з вокзалу на %s\\.\n%s\n\n%s";
    String chooseRightMenuOptionMessage = "Оберіть, будь ласка, один з пунктів меню";
}
