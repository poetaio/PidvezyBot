package services.message_services;

import org.telegram.abilitybots.api.util.AbilityUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EscapeMessageService {
    public static String escapeMessage(String mainMessage, String... escaping) {
        String[] escaped = new String[escaping.length];
        for (int i = 0; i < escaped.length; ++i) {
            escaped[i] = escapeString(escaping[i]);
        }
        return String.format(mainMessage, (Object[])escaped);
    }

    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("+", "\\+")
                .replace("~", "\\~")
                .replace("#", "\\#")
                .replace("|", "\\|")
                .replace("!", "\\!")
                .replace("*", "\\*")
                .replace("`", "\\`")
                .replace(".", "\\.")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace(">", "\\>")
                .replace("{", "\\{")
                .replace("}", "\\}");
    }
}
