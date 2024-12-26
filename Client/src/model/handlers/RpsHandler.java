package model.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.receive.Rps;
import model.messages.receive.RpsResult;

public class RpsHandler {
    private int currentStateOfGame = 0; // 0 - no game, 1 - sender, 2 - receiver
    private ChatHandler chatHandler;
    public RpsHandler(ChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    public void handleRps(String payload) throws InterruptedException, JsonProcessingException {
        Rps rps = Rps.fromJson(payload);
        if (chatHandler.isInChat()) {
            System.out.println("You received a request to play Rock-Paper-Scissors.against " + rps.opponent() + ". Type /rps <your_choice> (rock - 0, paper - 1, or scissors - 2) in order to respond.");
        } else {
            System.out.println("You received a request to play Rock-Paper-Scissors against " + rps.opponent() + ". Enter the chat first and type /rps <your_choice> (rock - 0, paper - 1, or scissors - 2).");
        }
        chatHandler.setReceivedRps(true);
        currentStateOfGame = 2; // this client is receiver
    }

    public void handleRpsResult(String payload) throws JsonProcessingException {
        RpsResult rpsResult = RpsResult.fromJson(payload);
        if (!rpsResult.status().isOk()) {
            int errorCode = rpsResult.status().code();
            switch (errorCode) {
                case 3000 -> System.out.println("You are not logged in.");
                case 3001 -> System.out.println("There is already a game in progress. Users playing: " + rpsResult.nowPlaying()[0] + ", " + rpsResult.nowPlaying()[1]);
                case 3002 -> System.out.println("There is no user with the username you specified as your opponent.");
                case 3003 -> System.out.println("You cannot play with yourself.");
                case 3004 -> System.out.println("You specified an incorrect choice code.");
                case 3005 -> System.out.println("The user you specified as your opponent specified an incorrect choice code.");
            }
            return;
        }

        String opponentChoiceStr;

        switch (rpsResult.opponentChoice()) {
            case 0 -> opponentChoiceStr = "rock";
            case 1 -> opponentChoiceStr = "paper";
            case 2 -> opponentChoiceStr = "scissors";
            default -> throw new IllegalStateException("Unexpected value: " + rpsResult.opponentChoice());
        }

        int gameResult = rpsResult.gameResult();

        // needed to switch the result of the game if the client is the receiver
        // in order to display the correct message
        if (currentStateOfGame == 2) {
            if (gameResult == 0)
                gameResult = 1;
            else if (gameResult == 1)
                gameResult = 0;
        }

        switch (gameResult) {
            case 0 -> System.out.println("You won! Opponent chose: " + opponentChoiceStr);
            case 1 -> System.out.println("You lost! Opponent chose: " + opponentChoiceStr);
            case 2 -> System.out.println("It's a tie!");
        }

    }
}
