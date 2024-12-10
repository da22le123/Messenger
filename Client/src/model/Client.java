package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.handlers.BroadcastHandler;
import model.handlers.EnterHandler;
import model.messages.MessageType;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.receive.Status;
import model.messages.receive.UserlistMessage;
import model.messages.send.EnterRequest;
import model.messages.send.Sendable;
import utils.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private String name;
    private final ReentrantLock lock;
    private boolean isLoggedIn;
    private final Condition loggedIn;
    private boolean isResponseReceived;
    private Condition responseReceived;
    private boolean isInChat;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;
    private final ArrayList<String> connectedUsers;
    private final BroadcastHandler broadcastHandler;
    private final EnterHandler enterHandler;

    public Client(String ipAddress, int port) throws IOException, InterruptedException {
        this.socket = setUpSocket(ipAddress, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        isLoggedIn = false;
        lock = new ReentrantLock();
        loggedIn = lock.newCondition();
        responseReceived = lock.newCondition();
        isInChat = false;
        unseenMessages = new ArrayList<>();
        connectedUsers = new ArrayList<>();
        broadcastHandler = new BroadcastHandler(out);
        enterHandler = new EnterHandler(out);
        setUpListenerThread().start();
        logIn();
    }


    /**
     * Set up a socket with the given IP address and port
     *
     * @param ipAddress IP address of the server
     * @param port      Port of the server
     * @return
     * @throws IOException
     */
    public Socket setUpSocket(String ipAddress, int port) throws IOException {
        return new Socket(ipAddress, port);
    }

    /**
     * Sends a message to the server
     *
     * @param message The message to be sent
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendMessage(Sendable message) throws JsonProcessingException {
        out.println(message.toJson());
    }


    /**
     * Set up a thread that listens for messages from the server
     *
     * @return The thread that listens for messages from the server
     */
    public Thread setUpListenerThread() {
        return new Thread(() -> {
            try {
                String line;
                // read the messages sent from the server
                // line == null when server hangs up
                while ((line = in.readLine()) != null) {
                    processMessage(line);
                }

                // handle the case when the server hangs up
                System.exit(0);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }


    public void logIn() throws InterruptedException, JsonProcessingException {
        enterHandler.logIn();
    }

    public void startChatting() throws JsonProcessingException {
        broadcastHandler.startChatting();
    }

    public void requestUserList() throws InterruptedException {
        lock.lock();
        connectedUsers.clear();

        sendMessage(MessageType.USERLIST_REQ.toString());

        while (!isResponseReceived) {
            responseReceived.await();
        }

        System.out.println("Connected users: \n");

        for (String user : connectedUsers) {
            // the connected user is this instance of the client app
            if (user.equals(name)) {
                System.out.println(user + " (you)");
                continue;
            }

            System.out.println(user);
        }

        System.out.println();

        lock.unlock();
    }

    /**
     * Exits the chat.
     * Send a BYE message to the server and terminates the program
     * Lock here is needed, otherwise client closes the connection before the BYE_RESP is received
     * Then server tries to send a message to a closed stream and throws an exception
     *
     * @throws IOException
     */
    public void exit() throws InterruptedException {
        lock.lock();
        sendMessage(MessageType.BYE.toString());

        while (!isResponseReceived) {
            responseReceived.await();
        }

        lock.unlock();
        System.exit(0);

    }

    /**
     * Processes the message received from the server based on the message type
     *
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    private void processMessage(String message) throws JsonProcessingException {
        // Split the message into two parts: the type and the rest
        String[] parts = message.split(" ", 2); // Limit to 2 splits
        // parse the message type
        MessageType messageType = MessageParser.parseMessageType(parts[0]);

        switch (messageType) {
            case PING -> sendMessage(MessageType.PONG.toString());

            case ENTER_RESP -> enterHandler.handleEnterResponse(parts[1]);

            case BROADCAST -> broadcastHandler.handleBroadcast(parts[1]);

            case BROADCAST_RESP -> broadcastHandler.handleBroadcastResponse(parts[1]);

            case USERLIST -> handleUserlist(parts[1]);

            case JOINED -> enterHandler.handleUserJoining(parts[1]);

            case LEFT -> handleUserLeaving(parts[1]);

            case BYE_RESP -> handleByeResponse();

            case PARSE_ERROR -> System.out.println("Parse error");

            case UNKNOWN_COMMAND -> System.out.println("Unknown command");
        }
    }

    // signal that the bye response has been received
    private void handleByeResponse() {
        lock.lock();

        System.out.println("Exiting the chat. See you next time!");
        isResponseReceived = true;
        responseReceived.signal();

        lock.unlock();
    }

    private void handleUserlist(String payload) throws JsonProcessingException {
        lock.lock();

        UserlistMessage userlistMessage = MessageParser.parseUserlist(payload);

        connectedUsers.clear();
        connectedUsers.addAll(userlistMessage.users());

        isResponseReceived = true;
        responseReceived.signal();

        lock.unlock();
    }

//    private void handleUserJoining(String message) throws JsonProcessingException {
//        ReceivedBroadcastMessage parsedMessage = MessageParser.parseMessage(message);
//        System.out.println(parsedMessage.username() + " has joined the chat.");
//    }

    /**
     * Handles the user leaving message received from the server.
     * Prints a message to the console that the user has left the chat.
     *
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    private void handleUserLeaving(String message) throws JsonProcessingException {
        ReceivedBroadcastMessage parsedMessage = MessageParser.parseMessage(message);
        System.out.println(parsedMessage.username() + " has left the chat.");
    }


    public void sendDirectMessage() throws JsonProcessingException, InterruptedException {
        lock.lock();
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the username of the user you want to send a message to: ");
        String recipient = sc.nextLine();

        System.out.println("Enter the message: ");
        String message = sc.nextLine();

        sendMessage(new model.messages.send.DirectMessageRequest(recipient, message));

        while (!isResponseReceived) {
            responseReceived.await();
        }
        lock.unlock();
    }

    private void handleDirectMessage(String payload) {

    }
}
