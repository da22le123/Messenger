package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.*;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.receive.Status;
import model.messages.send.BroadcastRequest;
import model.messages.send.EnterRequest;
import model.messages.send.Sendable;
import utils.MessageParser;

import java.io.*;
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
    private boolean isInChat;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;

    public Client(String ipAddress, int port) throws IOException, InterruptedException {
        this.socket = setUpSocket(ipAddress, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        isLoggedIn = false;
        lock = new ReentrantLock();
        loggedIn = lock.newCondition();
        isInChat = false;
        unseenMessages = new ArrayList<>();
        setUpListenerThread().start();
        logIn();
    }


    /**
     * Set up a socket with the given IP address and port
     * @param ipAddress IP address of the server
     * @param port Port of the server
     * @return
     * @throws IOException
     */
    public Socket setUpSocket(String ipAddress, int port) throws IOException {
        return new Socket(ipAddress, port);
    }

    /**
     * Sends a message to the server
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

    /**
     * Logs in the user
     * Awaits for the response from the server containing the status of the login
     * @throws InterruptedException
     */
    public void logIn() throws InterruptedException, JsonProcessingException {
        lock.lock();

            if (!isLoggedIn)
                // await for the condition
                while (!isLoggedIn) {
                    System.out.println("Enter your name: ");
                    Scanner sc = new Scanner(System.in);
                    name = sc.nextLine();

                    sendMessage(new EnterRequest(name));
                    loggedIn.await();
                }
            else
                System.out.println("You are already logged in");

        lock.unlock();
    }

    /**
     * Starts the chat with other users, first prints the unseen messages if there are any. Then awaits for the user input
     * If user types /quitchat, the chat ends
     */
    public void startChatting() throws JsonProcessingException {
        Scanner sc = new Scanner(System.in);

        System.out.println("The chat with other users has started. Type /quitchat to exit the chat.");
        isInChat = true;

        if (!unseenMessages.isEmpty()) {
            System.out.println("Unseen messages: ");
            for (ReceivedBroadcastMessage m : unseenMessages) {
                System.out.println(m.username() + ": " + m.message() + "\n");
            }
        }

        while (isInChat) {
            String message = sc.nextLine();

            if (message.equals("/quitchat")) {
                isInChat = false;
            } else {
                sendMessage(new BroadcastRequest(message));
            }
        }

        unseenMessages.clear();
    }

    /**
     * Exits the chat.|
     * Send a BYE message to the server and terminates the program
     * @throws IOException
     */
    public void exit() throws IOException {
        sendMessage(MessageType.BYE.toString());
        System.exit(0);
    }

    /**
     * Processes the message received from the server based on the message type
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

            case ENTER_RESP -> handleEnterResponse(parts[1]);

            case BROADCAST -> handleBroadcast(parts[1]);

            case BROADCAST_RESP -> handleBroadcastResponse(parts[1]);

            case LEFT -> handleUserLeaving(parts[1]);

            case BYE_RESP -> System.out.println("Exiting the chat. See you next time!");

            case PARSE_ERROR -> System.out.println("Parse error");

            case UNKNOWN_COMMAND -> System.out.println("Unknown command");
        }
    }

    /**
     * Handles the broadcast message received from the server.
     * If the user is not in the chat, the message is added to the unseen messages list.
     * If the user is in the chat, the message is printed to the console.
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    private void handleBroadcast(String message) throws JsonProcessingException {
        ReceivedBroadcastMessage parsedMessage = MessageParser.parseMessage(message);

        if (!isInChat) {
            unseenMessages.add(parsedMessage);
            System.out.println("There are new messages in the chat. You have " + unseenMessages.size() + " unseen messages. Enter the chat to see the messages.");
        } else {
            System.out.println(parsedMessage.username() + ": " + parsedMessage.message() + "\n");
        }
    }

    /**
     * Handles the broadcast response message received from the server.
     * If the message was not sent successfully, an error message is printed to the console.
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    private void handleBroadcastResponse(String message) throws JsonProcessingException {
        Status status = MessageParser.parseStatus(message);

        if (!status.isOk()) {
            System.out.println("Failed to send the message");
            switch (status.code()) {
                case 6000 -> System.out.println("User is not logged in.");
            }

        }
    }

    /**
     * Handles the enter response message received from the server.
     * If the user was successfully logged in, a success message is printed to the console.
     * If the user was not successfully logged in, an error message is printed to the console.
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    private void handleEnterResponse(String message) throws JsonProcessingException {
        lock.lock();

        Status status = MessageParser.parseStatus(message);

        if (status.isOk()) {
            System.out.println("Successfully logged in as " + name);
            isLoggedIn = true;
            loggedIn.signal();
        } else {
            System.out.println("Failed to log in as " + name);
            //signal without changing the state of the variable isLoggedIn
            loggedIn.signal();
            switch (status.code()) {
                case 5000 -> System.out.println("User with this name already exists");
                case 5001 -> System.out.println("Username has an invalid format or length");
                case 5002 -> System.out.println("Already logged in");
            }
        }

        lock.unlock();
    }

    /**
     * Handles the user leaving message received from the server.
     * Prints a message to the console that the user has left the chat.
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    private void handleUserLeaving(String message) throws JsonProcessingException {
        ReceivedBroadcastMessage parsedMessage = MessageParser.parseMessage(message);
        System.out.println(parsedMessage.username() + " has left the chat.");
    }



}
