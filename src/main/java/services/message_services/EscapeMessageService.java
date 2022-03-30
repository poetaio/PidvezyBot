package services.message_services;

import org.jetbrains.annotations.NotNull;

public class EscapeMessageService {
    public static String escapeMessageWithArgs(String mainMessage, String... escaping) {
        String[] escaped = new String[escaping.length];
        for (int i = 0; i < escaped.length; ++i) {
            escaped[i] = escapeString(escaping[i]);
        }
        return String.format(mainMessage, (Object[])escaped);
    }

    public static String escapeMessage(@NotNull String s) {
        return escapeString(s);
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
