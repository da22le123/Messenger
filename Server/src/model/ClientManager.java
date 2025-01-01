package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.send.RpsResult;
import model.messages.send.Sendable;
import model.messages.send.Status;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClientManager {
    private final List<ClientConnection> clients = new ArrayList<>();
    private final List<ClientConnection> awaitingAcceptance = new ArrayList<>();
    private RpsGame currentRpsGame;

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

    public synchronized String[] getNowPlaying() {
        return new String[]{currentRpsGame.getPlayer1().getUsername(), currentRpsGame.getPlayer2().getUsername()};
    }

    public synchronized boolean IsRpsGameRunning() {
        return currentRpsGame != null;
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

    public boolean isPlayingNow(ClientConnection client) {
        return currentRpsGame != null && (currentRpsGame.getPlayer1().equals(client) || currentRpsGame.getPlayer2().equals(client));
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
}
