package model.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.MessageSender;
import model.messages.Status;
import model.messages.receive.Ttt;
import model.messages.receive.TttMoveResponse;
import model.messages.receive.TttRequestReceive;
import model.messages.receive.TttResult;
import model.messages.send.TttMove;
import model.messages.send.TttResponse;

import java.util.Scanner;

public class TttHandler {
    private final ChatHandler chatHandler;
    private final MessageSender messageSender;
    // is needed to determine if the player is player1 or player2
    // player1 is the one who sends the request, player2 is the one who receives the request
    // the game result interpretation depends on this
    private boolean isPlayer1;  

    public TttHandler(ChatHandler chatHandler, MessageSender messageSender) {
        this.chatHandler = chatHandler;
        this.messageSender = messageSender;
    }




    public void handleTttResult(String payload) throws JsonProcessingException {
        TttResult tttResult = TttResult.fromJson(payload);

        if (!tttResult.status().isOk()) {
            switch (tttResult.status().code()) {
                case 2000 -> System.out.println("You are not logged in!");
                case 2001 -> System.out.println("There's already game going on on the server! Now playing: " + tttResult.nowPlaying()[0] + " and " + tttResult.nowPlaying()[1]);
                case 2002 -> System.out.println("You specified the non-existing opponent!");
                case 2003 -> System.out.println("You can't play against yourself!");
                case 2004 -> System.out.println("Illegal move!");
                case 2005 -> System.out.println("Your opponent has rejected the game request!");
            }
        } else {
            switch (tttResult.gameResult()) {
                case 0 -> {
                    if (isPlayer1) {
                        System.out.println("You won!");
                    }
                    else {
                        System.out.println("You lost!");
                    }
                }
                case 1 -> {
                    if (isPlayer1)
                        System.out.println("You lost!");
                    else
                        System.out.println("You won!");
                }
                case 2 -> System.out.println("It's a draw!");
            }

            printTttBoard(tttResult.board());
        }
        setIsPlayer1(false);
    }

    public void handleTttRequest(String payload) throws JsonProcessingException {
        TttRequestReceive tttRequest = TttRequestReceive.fromJson(payload);
        chatHandler.setReceivedTtt(true);

        if (chatHandler.isInChat()) {
            System.out.println("You received a request to play Tic-Tac-Toe against " + tttRequest.opponent() + ". Type /ttt_answer <yes/no> in order to respond.");
            printTttBoard(tttRequest.board());
        } else {
            System.out.println("You received a request to play Tic-Tac-Toe against " + tttRequest.opponent() +". Enter the chat first and type /ttt_answer <yes/no> in order to respond.");
            printTttBoard(tttRequest.board());
        }
    }

    public void handleTttMoveResponse(String payload) throws JsonProcessingException {
        TttMoveResponse tttMoveResponse = TttMoveResponse.fromJson(payload);

        if (tttMoveResponse.status().isOk()) {
            System.out.println("Your move has been accepted. The board: ");
            printTttBoard(tttMoveResponse.board());
        } 
        else if (tttMoveResponse.status().code()==2004){
            System.out.println("Your move was illegal! Try again via /ttt_move <move> command!");
        }
    }

    public void handleTtt(String payload) throws JsonProcessingException {
        Ttt ttt = Ttt.fromJson(payload);
        if (!chatHandler.isInChat()) {
            System.out.println("Your ttt game opponent has made a move, below you will see the current state of board!");
            printTttBoard(ttt.board());
            System.out.println("In order to make your move, enter the chat and use /ttt_move <move> command. You are not in the chat!");
        } else {
            System.out.println("Your ttt game opponent has made a move, below you will see the current state of board!");
            printTttBoard(ttt.board());
            System.out.println("In order to make your move, use /ttt_move <move> command.");
        }
    }


    public void printTttBoard(String[] board) {
        System.out.println("Current state of the board: ");
        System.out.println();
        for (int i = 0; i < board.length; i++) {
            System.out.print(board[i] + " ");
            if ((i + 1) % 3 == 0) {
                System.out.println();
            }
        }
    }

    public void setIsPlayer1(boolean isPlayer1) {
        this.isPlayer1 = isPlayer1;
    }
}
