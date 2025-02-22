package model;

import java.util.Arrays;

public class TttGame {
    // the one that requested the game
    private ClientConnection player1;
    // the one that received the game request
    private ClientConnection player2;
    private ClientConnection nextPlayerToMove;

    /**
     * Constructs a new Tic Tac Toe game with two players.
     *
     * @param player1 the player who requested the game
     * @param player2 the player who accepted the game request
     */
    public TttGame(ClientConnection player1, ClientConnection player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.nextPlayerToMove = player1;
    }

    /**
     * Returns the username of player1.
     *
     * @return the username of player1
     */
    public ClientConnection getPlayer1() {
        return player1;
    }

    /**
     * Returns the username of player2.
     *
     * @return the username of player2
     */
    public ClientConnection getPlayer2() {
        return player2;
    }

    private String[] board = {".", ".", ".", ".", ".", ".", ".", ".", "."};

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
            if (!board[combo[0]].equals(".") &&
                    board[combo[0]].equals(board[combo[1]]) &&
                    board[combo[1]].equals(board[combo[2]])) {
                return board[combo[0]];  // Return the winner ("X" or "O")
            }
        }

        if (isBoardFull()) {
            return "draw";
        }

        return ".";  // No winner yet
    }

    /**
     * Checks if the board is completely filled.
     *
     * @return true if there are no empty cells, false otherwise
     */
    public boolean isBoardFull() {
        for (String cell : board) {
            if (cell.equals(".")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the current board.
     *
     * @return the board as an array of Strings
     */
    public String[] getBoard() {
        return board;
    }

    public void setBoard(String[] board) {
        this.board = board;
    }

    public String[] getCurrentPlayerUsernames() {
        return new String[]{player1.getUsername(), player2.getUsername()};
    }

    public ClientConnection getNextPlayerToMove() {
        return nextPlayerToMove;
    }

    /**
     * Sets the next player to move.
     * If the specified player is not one of the players in the game, the method does nothing.
     * @param player the player who will make the next move
     */
    public void setNextPlayerToMove(ClientConnection player) {
        if (!player.equals(player1) && !player.equals(player2)) {
            return;
        }

        nextPlayerToMove = player;
    }

    /**
     * Swaps the next player to move.
     */
    public void swapNextPlayerToMove() {
        nextPlayerToMove = nextPlayerToMove.equals(player1) ? player2 : player1;
    }

    /**
     * Validates that an opponent's board update is valid.
     *
     * The update is considered valid if:
     * <ul>
     *   <li>The new board has exactly one additional move (i.e. one cell that was "." is now non-empty).</li>
     *   <li>The new move was made in an empty cell (no previous move was overwritten).</li>
     *   <li>The new cell contains the symbol that is expected.
     *       According to your spec, if nextPlayerToMove is player1 then the new cell must be "X";
     *       if nextPlayerToMove is player2 then it must be "O".</li>
     *   <li>All other cells remain unchanged and only valid symbols (".", "X", "O") are present.</li>
     * </ul>
     *
     * @param newBoard a 1D array of Strings representing the updated board.
     * @return true if the opponent’s move is valid; false otherwise.
     */
    public boolean isValidOpponentMove(String[] newBoard) {
        // Check for null or wrong length
        if (newBoard == null || newBoard.length != board.length) {
            return false;
        }

        // Count non-empty cells in both boards.
        int countOld = 0;
        int countNew = 0;
        for (int i = 0; i < board.length; i++) {
            // Validate current board cell symbols.
            if (!board[i].equals(".") && !board[i].equals("X") && !board[i].equals("O")) {
                return false;
            }
            // Validate new board cell symbols.
            if (!newBoard[i].equals(".") && !newBoard[i].equals("X") && !newBoard[i].equals("O")) {
                return false;
            }
            if (!board[i].equals(".")) {
                countOld++;
            }
            if (!newBoard[i].equals(".")) {
                countNew++;
            }
            // No previous move should be overwritten.
            // If the old cell is non-empty, it must remain unchanged.
            if (!board[i].equals(".") && !board[i].equals(newBoard[i])) {
                return false;
            }
        }
        // The new board must have exactly one additional move.
        if (countNew != countOld + 1) {
            return false;
        }

        // Determine the expected symbol for the new move.
        // According to your specification:
        // - If nextPlayerToMove is player1, then the new cell should contain "X".
        // - If nextPlayerToMove is player2, then it should be "O".
        String expectedSymbol = nextPlayerToMove.equals(player1) ? "X" : "O";

        // Identify the cell that changed and verify it holds the expected symbol.
        boolean foundDifference = false;
        for (int i = 0; i < board.length; i++) {
            if (board[i].equals(".") && !newBoard[i].equals(".")) {
                // Found a cell that changed. It must match the expected symbol.
                if (!newBoard[i].equals(expectedSymbol)) {
                    return false;
                }
                // Ensure that only one cell was modified.
                if (foundDifference) { // This would indicate more than one change.
                    return false;
                }
                foundDifference = true;
            }
        }

        // If no difference was found, then no move was made.
        return foundDifference;
    }

    public int getNumberOfMovesOnTheBoard() {
        int count = 0;
        for (String cell : board) {
            if (!cell.equals(".")) {
                count++;
            }
        }
        return count;
    }
}
