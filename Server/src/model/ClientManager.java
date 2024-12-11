package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.send.Sendable;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ClientManager {
    private final List<ClientConnection> clients = new ArrayList<>();
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

    public synchronized void startRpsGame(ClientConnection player1, ClientConnection player2, int choicePlayer1) {
        currentRpsGame = new RpsGame(player1, player2, choicePlayer1);
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

    public synchronized void setChoicePlayer2(int choicePlayer2) {
        currentRpsGame.setChoicePlayer2(choicePlayer2);
    }

    public synchronized int getPlayer2Choice() {
        return currentRpsGame.getChoicePlayer2();
    }

    public synchronized void calculateGameResult() {
        currentRpsGame.calculateGameResult();
    }

    public synchronized int getGameResult() {
        return currentRpsGame.getGameResult();
    }
}
