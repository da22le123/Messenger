package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.send.RpsResult;
import model.messages.send.Sendable;
import model.messages.send.Status;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ClientManager {
    private final List<ClientConnection> clients = new ArrayList<>();

    private final List<ClientConnection> awaitingAcceptance = new ArrayList<>();

    private RpsGame currentRpsGame;

    private TttGame currentTttGame;

    public synchronized boolean addClient(ClientConnection client) {
        if (hasClient(client)) {
            return false;
        }

        clients.add(client);
        return true;
    }

    public synchronized void removeClient(ClientConnection client) {
        clients.remove(client);
    }

    private synchronized boolean hasClient(ClientConnection clientConnection) {
        return clients.contains(clientConnection);
    }

    public synchronized boolean hasClient(String username) {
        ClientConnection client = clients.stream().filter(clientConnection -> clientConnection.getUsername().equals(username)).findAny().orElse(null);

        return client != null;
    }

    public synchronized ClientConnection getClientByUsername(String username) {
        return clients.stream().filter(clientConnection -> clientConnection.getUsername().equals(username)).findAny().orElse(null);
    }

    public synchronized List<ClientConnection> getClients() {
        return new ArrayList<>(clients);
    }

    public synchronized void addPlayer2Choice(int choicePlayer2) {
        currentRpsGame.setChoicePlayer2(choicePlayer2);
    }

    public synchronized void sendMessageToAllClients(Sendable message, ClientConnection sender) throws JsonProcessingException {
        for (ClientConnection client : clients) {
            // don't send the message to the sender
            if (client.equals(sender)) {
                continue;
            }

            PrintWriter out = client.getPrintWriter();
            out.println(message.toJson());
            System.out.println("Sent message: " + message.toJson());
        }
    }

    public synchronized String[] getNowPlayingRps() {
        return new String[]{currentRpsGame.getPlayer1().getUsername(), currentRpsGame.getPlayer2().getUsername()};
    }

    public synchronized boolean IsRpsGameRunning() {
        return currentRpsGame != null;
    }

    public synchronized boolean IsTttGameRunning() {
        return currentTttGame != null;
    }

    public synchronized void startRpsGame(String usernamePlayer1, String usernamePlayer2, int choicePlayer1) {
        ClientConnection player1 = getClientByUsername(usernamePlayer1);
        ClientConnection player2 = getClientByUsername(usernamePlayer2);
        currentRpsGame = new RpsGame(player1, player2, choicePlayer1);
    }

    public synchronized void calculateGameResult() {
        currentRpsGame.calculateGameResult();
    }

    public synchronized void sendRpsResultToPlayers(Status status) throws JsonProcessingException {
        int result = currentRpsGame.getGameResult();
        PrintWriter player1 = currentRpsGame.getPlayer1().getPrintWriter();
        PrintWriter player2 = currentRpsGame.getPlayer2().getPrintWriter();
        RpsResult rpsResultForPlayer1 = new RpsResult(status, result, currentRpsGame.getChoicePlayer2());
        RpsResult rpsResultForPlayer2 = new RpsResult(status, result, currentRpsGame.getChoicePlayer1());

        player1.println(rpsResultForPlayer1.toJson());
        player2.println(rpsResultForPlayer2.toJson());

        System.out.println("Sending message: " + rpsResultForPlayer1.toJson());

    }

    public void abortRpsGame() {
        currentRpsGame = null;
    }

    public void abortTttGame() {
        currentTttGame = null;
    }

    public boolean isPlayingRpsNow(ClientConnection client) {
        return currentRpsGame != null && (currentRpsGame.getPlayer1().equals(client) || currentRpsGame.getPlayer2().equals(client));
    }

    /**
     * Get current players of the ttt game
     * @return the username of the player that is currently playing, or empty array if no game is running
     */
    public synchronized String[] getNowPlayingTtt() {
        if (!IsTttGameRunning()) {
            return new String[]{}; // empty array
        }

        return currentTttGame.getCurrentPlayerUsernames();
    }

    public synchronized void addAwaitingAcceptanceClient(ClientConnection client) {
        awaitingAcceptance.add(client);
    }

    public synchronized void removeAwaitingAcceptanceClient(ClientConnection client) {
        awaitingAcceptance.remove(client);
    }

    public synchronized boolean isAwaitingAcceptance(String client) {
        return awaitingAcceptance.stream().anyMatch(clientConnection -> clientConnection.getUsername().equals(client));
    }

    public synchronized TttGame getCurrentTttGame() {
        return currentTttGame;
    }

    public synchronized void startTttGame(ClientConnection player1, ClientConnection player2) {
        currentTttGame = new TttGame(player1, player2);
    }

    public synchronized boolean addTttMove(int move, ClientConnection player) {
        if (currentTttGame.getNextPlayerToMove() != player) {
            return false;
        }

        String playerSymbol = "";

        if (player.equals(currentTttGame.getPlayer1())) {
            playerSymbol = "X";
        } else if (player.equals(currentTttGame.getPlayer2())) {
            playerSymbol = "O";
        }

        return currentTttGame.makeMove(move, playerSymbol);
    }

    public synchronized void setNextPlayerToMove(ClientConnection player) {
        currentTttGame.setNextPlayerToMove(player);
    }

    public void swapNextPlayerToMove() {
        currentTttGame.swapNextPlayerToMove();
    }

    /**
     * Returns the current game result.
     *
     * @return 0 - player1 won, 1 - player2 won, 2 - draw, -1 - game is not over yet
     */
    public int getTttResult() {
        switch (currentTttGame.checkWinner()) {
            case "X" -> {
                return 0;
            }
            case "O" -> {
                return 1;
            }
            case "draw" -> {
                return 2;
            }
            default -> {
                return -1;
            }
        }
    }

    public synchronized ClientConnection getTttOpponent (ClientConnection player) {
        if (currentTttGame.getPlayer1().equals(player)) {
            return currentTttGame.getPlayer2();
        } else if (currentTttGame.getPlayer2().equals(player)) {
            return currentTttGame.getPlayer1();
        }

        return null;
    }
}
