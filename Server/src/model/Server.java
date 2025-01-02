package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final ClientManager clientManager;
    private final ServerSocket serverSocketTextBased;
    private final ServerSocket serverSocketFileTransfer;

    public Server(int port) throws IOException {
        serverSocketTextBased = setUpServerSocket(port);
        // The file-transfer server listens on the next port.
        serverSocketFileTransfer = setUpServerSocket(port + 1);
        clientManager = new ClientManager();
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
            ClientConnection clientConnection = new ClientConnection(socket, clientManager);

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
                    getUUIDnUpdateTransfersMap(socket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void getUUIDnUpdateTransfersMap(Socket fileTransferSocket) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileTransferSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(fileTransferSocket.getOutputStream(), true);
                String message = reader.readLine();

                String[] parts = message.split("_");
                if (parts.length < 2) {
                    System.err.println("Invalid file-transfer identifier: " + message);
                    return;
                }

                String uuid = parts[0];
                String mode = parts[1]; // "send" or "receive"

                if (!clientManager.hasTransfer(uuid)) {
                    System.err.println("Unknown client: " + uuid);
                    return;
                }

                switch (mode) {
                    case "send":
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(fileTransferSocket.getOutputStream()));
                        clientManager.setSender(uuid, out);
                        break;
                    case "receive":
                        BufferedReader in = new BufferedReader(new InputStreamReader(fileTransferSocket.getInputStream()));
                        clientManager.setReceiver(uuid, in);
                        break;
                    default:
                        System.err.println("Invalid file-transfer mode: " + mode);
                        break;
                }

                if (clientManager.isTransferReady(uuid)) {
                    new Thread(() -> {
                        clientManager.startTransfer(uuid);
                    }).start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


}
