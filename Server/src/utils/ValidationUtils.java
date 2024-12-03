package utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,14}$");

    public static boolean isValidUsername(String input) {
        // Match against the pattern and return true if it matches
        return VALID_USERNAME_PATTERN.matcher(input).matches();
    }
}
