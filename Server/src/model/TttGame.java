package model;

public class TttGame {
    private String[] board = {"-", "-", "-", "-", "-", "-", "-", "-", "-"};

    /**
     * Checks if the move is legal.
     * A legal move must be within 0-8 and the chosen cell must be empty ("-").
     *
     * @param move the board index (0-8)
     * @return true if the move is legal, false otherwise
     */
    public boolean isLegalMove(int move) {
        if (move < 0 || move > 8) {
            return false;
        }

        return board[move].equals("-");
    }

    /**
     * Makes a move on the board for the specified player.
     *
     * @param move the board index (0-8)
     * @param player the player's symbol ("X" or "O")
     * @return true if the move was made successfully, false if the move was illegal
     */
    public boolean makeMove(int move, String player) {
        if (!isLegalMove(move)) {
            return false;
        }
        board[move] = player;
        return true;
    }

    /**
     * Checks if there is a winner.
     *
     * @return "X" or "O" if one of the players has won, "draw" if the board is full and has no winner,
     *         or "-" if the game is still ongoing.
     */
    public String checkWinner() {
        // All possible winning combinations: rows, columns, and diagonals.
        int[][] winningCombos = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},  // rows
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},  // columns
                {0, 4, 8}, {2, 4, 6}              // diagonals
        };

        for (int[] combo : winningCombos) {
            if (!board[combo[0]].equals("-") &&
                    board[combo[0]].equals(board[combo[1]]) &&
                    board[combo[1]].equals(board[combo[2]])) {
                return board[combo[0]];  // Return the winner ("X" or "O")
            }
        }

        if (isBoardFull()) {
            return "draw";
        }

        return "-";  // No winner yet
    }

    /**
     * Checks if the board is completely filled.
     *
     * @return true if there are no empty cells, false otherwise
     */
    public boolean isBoardFull() {
        for (String cell : board) {
            if (cell.equals("-")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resets the board to its initial state.
     */
    public void resetBoard() {
        for (int i = 0; i < board.length; i++) {
            board[i] = "-";
        }
    }

    /**
     * Returns the current board.
     *
     * @return the board as an array of Strings
     */
    public String[] getBoard() {
        return board;
    }
}
