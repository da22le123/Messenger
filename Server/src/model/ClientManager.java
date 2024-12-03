package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.send.Sendable;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ClientManager {
    private final List<ClientConnection> clients = new ArrayList<>();

    public synchronized boolean addClient(ClientConnection client) {
        if (hasClient(client)) {
            return false;
        }

        clients.add(client);
        return true;
    }

    private synchronized boolean hasClient(ClientConnection clientConnection) {
        return clients.contains(clientConnection);
    }

    public synchronized boolean hasClient(String username) {
        ClientConnection client = clients.stream().filter(clientConnection -> clientConnection.getUsername().equals(username)).findAny().orElse(null);

        return client != null;
    }

    public synchronized List<ClientConnection> getClients() {
        return clients;
    }

    public synchronized void sendMessageToAllClients(Sendable message, ClientConnection sender) throws JsonProcessingException {
        for (ClientConnection client : clients) {
            System.out.println("Clients size: " + clients.size());
            System.out.println("Client: " + client.getUsername());
            // don't send the message to the sender
            if (client.equals(sender)) {
                continue;
            }

            PrintWriter out = client.getPrintWriter();
            out.println(message.toJson());
            System.out.println("Sent message: " + message.toJson());
        }
    }

}
