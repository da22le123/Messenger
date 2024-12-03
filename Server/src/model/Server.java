package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private final ClientManager clientManager;
    private final ServerSocket serverSocket;

    public Server(int port) throws IOException {
        serverSocket = setUpServerSocket(port);
        clientManager = new ClientManager();
        handleIncomingConnections();
    }
    private ServerSocket setUpServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    private void handleIncomingConnections() throws IOException {
        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();
            ClientConnection clientConnection = new ClientConnection(socket);

            // For each client start a processing thread and a ping thread.
            clientConnection.startMessageProcessingThread();
        }
    }




}
