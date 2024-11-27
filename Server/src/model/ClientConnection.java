package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.MessageType;
import model.messages.receive.EnterRequest;
import utils.MessageParser;
import utils.ValidationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ClientConnection {
    private String username;
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    private ReentrantLock lock;
    private Condition receivedPong;
    private boolean isPongReceived;

    private final ScheduledExecutorService scheduler;
    private final ClientManager clientManager;


    public ClientConnection(String username, Socket socket) throws IOException {
        this.username = username;
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        scheduler = Executors.newSingleThreadScheduledExecutor();
        clientManager = new ClientManager();
    }

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        scheduler = Executors.newSingleThreadScheduledExecutor();
        clientManager = new ClientManager();
    }

    public void startMessageProcessingThread() {
        new Thread(() -> {
            try {
                //todo create a data structure for the message
                sendMessage("READY {\"version\": \"1.0.0\"}");

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("Received message: " + clientMessage);
                    processMessage(clientMessage);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void processMessage(String clientMessage) throws JsonProcessingException {
        // Split the message into two parts: the type and the rest
        String[] parts = clientMessage.split(" ", 2); // Limit to 2 splits
        // parse the message type
        MessageType messageType = MessageParser.parseMessageType(parts[0]);

        switch (messageType) {
            case ENTER -> handleEnter(parts[1]);
            case PONG -> isPongReceived = true;
        }
    }

    public void startPingThread() {
        Thread pingThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                sendMessage("PING");
                long startTime = System.currentTimeMillis();
                while (!isPongReceived) {
                    if (System.currentTimeMillis() - startTime > 3000) {
                        hangUp(); // todo not yet implemented
                        break;
                    }
                }

                isPongReceived = false;
            }
        });

        pingThread.start();
    }


    private void hangUp() {
        // todo implement
    }

    private void handleEnter(String payload) throws JsonProcessingException {
        EnterRequest enterRequest = EnterRequest.fromJson(payload);
        String proposedUsername = enterRequest.username();

        // already has a client with the same username
        if (clientManager.hasClient(proposedUsername)) {
            sendMessage("ENTER_RESP {\"status\":\"ERROR\", \"code\":5000");
            return;
        }

        // invalid username
        if (ValidationUtils.isValidUsername(proposedUsername)) {
            sendMessage("ENTER_RESP {\"status\":\"ERROR\", \"code\":5001}");
            return;
        }

        // already logged in
        if (this.username != null) {
            sendMessage("ENTER_RESP {\"status\":\"ERROR\", \"code\":5002}");
            return;
        }

        setUsername(proposedUsername);
        sendMessage("ENTER_RESP {\"status\":\"OK\"}");
        startPingThread();
    }














    public void sendMessage(String message) {
        System.out.println("Sending message: " + message);
        out.println(message);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getPrintWriter() {
        return out;
    }

    public BufferedReader getBufferedReader() {
        return in;
    }


}
