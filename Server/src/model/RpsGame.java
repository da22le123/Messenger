package model;

public class RpsGame {
    // the one that requested the game
    private ClientConnection player1;
    // the one that received the game request
    private ClientConnection player2;
    // possible choices are:
    // 0	Rock, beats scissors, looses to paper
    // 1	Paper, beats rock, looses to scissors
    // 2	Scissors, beats paper, losses to rock
    private int choicePlayer1;
    private int choicePlayer2;
    private int gameResult;
    public RpsGame(ClientConnection player1, ClientConnection player2, int choicePlayer1) {
        this.player1 = player1;
        this.player2 = player2;
        this.choicePlayer1 = choicePlayer1;
    }

    public ClientConnection getPlayer1() {
        return player1;
    }

    public ClientConnection getPlayer2() {
        return player2;
    }

    public int getChoicePlayer1() {
        return choicePlayer1;
    }

    public int getChoicePlayer2() {
        return choicePlayer2;
    }

    public int getGameResult() {
        return gameResult;
    }

    public void setPlayer2(ClientConnection player2) {
        this.player2 = player2;
    }

    public void setChoicePlayer2(int choicePlayer2) {
        this.choicePlayer2 = choicePlayer2;
    }


    /**
     * Calculate the result of the game
     * 0	- The client that had started the game (C1) has won
     * 1	- The client that was challenged to play (C2) has won
     * 2	- The game ended with draw
     */
    public void calculateGameResult() {
        if (choicePlayer1 == choicePlayer2) {
            gameResult = 2;
        } else if (choicePlayer1 == 0 && choicePlayer2 == 2) {
            gameResult = 0;
        } else if (choicePlayer1 == 1 && choicePlayer2 == 0) {
            gameResult = 0;
        } else if (choicePlayer1 == 2 && choicePlayer2 == 1) {
            gameResult = 0;
        } else {
            gameResult = 1;
        }
    }
}
