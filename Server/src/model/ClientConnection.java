package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.MessageType;
import model.messages.messagefactories.EnterResponseFactory;
import model.messages.receive.EnterRequest;
import model.messages.send.JoinedMessage;
import model.messages.send.Response;
import model.messages.send.Sendable;
import utils.MessageParser;
import utils.ValidationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ClientConnection {
    private String username;
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    //private boolean isPongReceived;

    private ClientManager clientManager;


    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition pongReceived = lock.newCondition();
    private volatile boolean isPongReceived = false;


    public ClientConnection(Socket socket, ClientManager clientManager) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.clientManager = clientManager;
    }

    public void startMessageProcessingThread() {
        new Thread(() -> {
            try {
                //todo create a data structure for the message

                sendMessage("READY {\"version\": \"1.6.0\"}");

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
            case PONG -> handlePong();
            case BROADCAST_REQ -> handleBroadcast(parts[1]);
        }
    }

    private void handleBroadcast(String payload) {

    }

    public void handlePong() {
        lock.lock();
        try {
            isPongReceived = true;
            pongReceived.signal();
        } finally {
            lock.unlock();
        }
    }

    public void pingPong() {
        lock.lock();

        while (!isPongReceived) {
            sendMessage("PING");
            try {
                // hangup if we do not receive pong in 3 seconds
                if (!pongReceived.await(3, TimeUnit.SECONDS)) {
                    hangUp();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        isPongReceived = false;
        lock.unlock();
    }

    public void startPingScheduler() {
        scheduler.scheduleAtFixedRate(this::pingPong, 10, 10, TimeUnit.SECONDS);
    }


    private void hangUp() {
        // todo implement
        System.out.println("Hanging up");
    }

    private void handleEnter(String payload) throws JsonProcessingException {
        EnterRequest enterRequest = EnterRequest.fromJson(payload);
        String proposedUsername = enterRequest.username();

        boolean hasClient = clientManager.hasClient(proposedUsername);
        boolean isInvalidUsername = proposedUsername.isEmpty() || !ValidationUtils.isValidUsername(proposedUsername);
        boolean isLoggedIn = this.username != null;

        EnterResponseFactory enterResponseFactory = new EnterResponseFactory();
        Response response = enterResponseFactory.createEnterResponse(hasClient, isInvalidUsername, isLoggedIn, proposedUsername);

        if (response != null) {
            sendMessage(response);
        } else {
            throw new RuntimeException("No response was created by the factory.");
        }

        if (response.status().isOk()) {
            // set the current username
            this.username = proposedUsername;
            // add the client to the list of clients
            clientManager.addClient(this);
            // notify all clients that a new client has entered
            clientManager.sendMessageToAllClients(new JoinedMessage(this.username), this);
        }

        startPingScheduler();
    }

    private void sendMessage(String message) {
        System.out.println("Sending message: " + message);
        out.println(message);
    }

    private void sendMessage(Sendable message) {
        try {
            sendMessage(message.toJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public boolean equals(Object obj) {
        if (this.getUsername() != null && ((ClientConnection) obj).getUsername() != null) {
            return this.getUsername().equals(((ClientConnection) obj).getUsername());
        }

        return this.getSocket().equals(((ClientConnection) obj).getSocket());
    }
}
