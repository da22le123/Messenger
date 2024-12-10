package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.MessageType;
import model.messages.messagefactories.StatusFactory;
import model.messages.receive.BroadcastRequest;
import model.messages.receive.EnterRequest;
import model.messages.send.*;
import utils.MessageParser;
import utils.ValidationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
    private volatile boolean isPingSent = false;


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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void processMessage(String clientMessage) throws JsonProcessingException, InterruptedException {
        // Split the message into two parts: the type and the rest
        String[] parts = clientMessage.split(" ", 2); // Limit to 2 splits
        // parse the message type
        MessageType messageType = MessageParser.parseMessageType(parts[0]);

        switch (messageType) {
            case ENTER -> handleEnter(parts[1]);
            case PONG -> handlePong();
            case BROADCAST_REQ -> handleBroadcast(parts[1]);
            case USERLIST_REQ -> handleUserlist();
            case BYE -> handleBye();
        }
    }

    private void handleUserlist() {
        List<String> usernames =  clientManager.getClients().stream()
                .map(ClientConnection::getUsername)
                .toList();

        sendMessage(new UserlistMessage(usernames));
    }

    private void handleBroadcast(String payload) throws JsonProcessingException {
        BroadcastRequest broadcastRequest = BroadcastRequest.fromJson(payload);

        boolean isLoggedIn = this.username != null;

        StatusFactory statusFactory = new StatusFactory();
        Status status = statusFactory.createBroadcastResponseStatus(isLoggedIn);

        if (status != null) {
            sendMessage(new Response(MessageType.BROADCAST_RESP, status));
        } else {
            throw new RuntimeException("No response was created by the factory.");
        }

        if (status.isOk()) {
            clientManager.sendMessageToAllClients(new BroadcastMessage(this.username, broadcastRequest.message()), this);
        }
    }

    private void handleBye() throws JsonProcessingException, InterruptedException {
        sendMessage(new Response(MessageType.BYE_RESP, new Status("OK", 0)));
        clientManager.sendMessageToAllClients(new LeftMessage(this.username), this);

        clientManager.removeClient(this);
        scheduler.shutdownNow();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);

        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePong() {
        lock.lock();
        try {
            if (isPingSent) {
                isPongReceived = true;
                pongReceived.signal();
            } else {
                sendMessage(new PongErrorMessage(8000));
            }
        } finally {
            lock.unlock();
        }
    }

    public void pingPong() {
        lock.lock();

        while (!isPongReceived) {
            sendMessage("PING");
            isPingSent = true;
            try {
                // hangup if we do not receive pong in 3 seconds
                if (!pongReceived.await(3, TimeUnit.SECONDS)) {
                    hangUp();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        isPingSent = false;
        isPongReceived = false;
        lock.unlock();
    }

    public void startPingScheduler() {
        scheduler.scheduleAtFixedRate(this::pingPong, 10, 10, TimeUnit.SECONDS);
    }


    private void hangUp() throws InterruptedException, JsonProcessingException {
        sendMessage(new HangupMessage(7000));
        clientManager.sendMessageToAllClients(new LeftMessage(this.username), this);

        clientManager.removeClient(this);
        scheduler.shutdownNow();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);

        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEnter(String payload) throws JsonProcessingException {
        EnterRequest enterRequest = EnterRequest.fromJson(payload);

        if (enterRequest == null) {
            sendMessage(MessageType.PARSE_ERROR.toString());
            return;
        }

        String proposedUsername = enterRequest.username();

        boolean hasClient = clientManager.hasClient(proposedUsername);
        boolean isInvalidUsername = proposedUsername.isEmpty() || !ValidationUtils.isValidUsername(proposedUsername);
        boolean isLoggedIn = this.username != null;

        StatusFactory statusFactory = new StatusFactory();
        Status status = statusFactory.createEnterResponseStatus(hasClient, isInvalidUsername, isLoggedIn, proposedUsername);

        if (status != null) {
            sendMessage(new Response(MessageType.ENTER_RESP, status));
        } else {
            throw new RuntimeException("No response was created by the factory.");
        }

        if (status.isOk()) {
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
