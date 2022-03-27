package services.message_services;

import org.jetbrains.annotations.NotNull;

public class NormalizeService {
    public static String normalizeNumber(String number) {
        if (number == null) {
            return "";
        } else if (number.indexOf('+') != 0 && number.length() > 0) {
            return '+' + number;
        }
        return number;
    }

    // should be called after normalizing number (so that it wouldn't be null)
    public static String normalizeUsername(String username, @NotNull String number) {
        if (username == null) {
            if (number.equals("")) {
                return "";
            }
            return "https://t.me/" + number;
        }
        return "@" + username;
    }
}
