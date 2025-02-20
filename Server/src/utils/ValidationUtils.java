package utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,14}$");

    public static boolean isValidUsername(String input) {
        // Match against the pattern and return true if it matches
        return VALID_USERNAME_PATTERN.matcher(input).matches();
    }

    public static boolean isValidBoard(String[] board) {
        // Check if the board has 9 elements
        if (board.length != 9) {
            return false;
        }
        // Check if the board has only valid elements
        for (String cell : board) {
            if (!cell.equals(".") && !cell.equals("X") && !cell.equals("O")) {
                return false;
            }
        }
        return true;
    }

}
