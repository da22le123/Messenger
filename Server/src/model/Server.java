package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final ClientManager clientManager;
    private final FileTransferManager fileTransferManager;
    private final ServerSocket serverSocketTextBased;
    private final ServerSocket serverSocketFileTransfer;

    public Server(int port) throws IOException {
        serverSocketTextBased = setUpServerSocket(port);
        // The file-transfer server listens on the next port.
        serverSocketFileTransfer = setUpServerSocket(port + 1);
        clientManager = new ClientManager();
        fileTransferManager = new FileTransferManager();
        handleIncomingFileTransferConnections().start();
        handleIncomingConnections();
    }

    private ServerSocket setUpServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    private void handleIncomingConnections() throws IOException {
        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocketTextBased.accept();
            ClientConnection clientConnection = new ClientConnection(socket, clientManager, fileTransferManager);

            // For each client start a processing thread and a ping thread.
            clientConnection.startMessageProcessingThread();
        }
    }

//    private void handleIncomingFileTransferConnections() throws IOException {
//        System.out.println("File-transfer server socket started.");
//        while (true) {
//            // Wait for an incoming client-connection request (blocking).
//            Socket socket = serverSocketFileTransfer.accept();
//            // for each incoming file-transfer connection, update the transfers map and start a file transfer if needed
//            getUUIDnUpdateTransfersMap(socket);
//        }
//    }

    private Thread handleIncomingFileTransferConnections() {
        return new Thread(() -> {
            while (true) {
                // Wait for an incoming client-connection request (blocking).
                try {
                    // for each incoming file-transfer connection, update the transfers map and start a file transfer if needed
                    Socket socket = serverSocketFileTransfer.accept();
                    fileTransferManager.getUUIDnUpdateTransfersMap(socket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }



}
