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

        return board[move].equals(".");
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
}
