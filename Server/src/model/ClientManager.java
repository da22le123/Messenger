package model;

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

}
