package utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[A-Z0-9_]{3,14}$", Pattern.CASE_INSENSITIVE);

    public static boolean isValidUsername(String input) {
        return !VALID_USERNAME_PATTERN.matcher(input).matches();
    }
}
