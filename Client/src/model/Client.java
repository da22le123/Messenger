package model;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final ArrayList<Message> unseenMessages;

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


    public Socket setUpSocket(String ipAddress, int port) throws IOException {
        return new Socket(ipAddress, port);
    }

    public void sendMessage(String message) {
        out.println(message);
    }


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
                System.out.println("Server hung up, connection lost.");
                System.exit(0);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public void logIn() throws InterruptedException {
        lock.lock();

            if (!isLoggedIn)
                // await for the condition
                while (!isLoggedIn) {
                    System.out.println("Enter your name: ");
                    Scanner sc = new Scanner(System.in);
                    name = sc.nextLine();

                    sendMessage("ENTER {\"username\":\"" + name + "\"}");
                    loggedIn.await();
                }
            else
                System.out.println("You are already logged in");

        lock.unlock();
    }

    public void startChatting() {
        Scanner sc = new Scanner(System.in);

        System.out.println("The chat with other users has started. Type /quitchat to exit the chat.");
        isInChat = true;

        if (!unseenMessages.isEmpty()) {
            System.out.println("Unseen messages: ");
            for (Message m : unseenMessages) {
                System.out.println(m.username() + ": " + m.message() + "\n");
            }
        }

        while (isInChat) {
            String message = sc.nextLine();

            if (message.equals("/quitchat")) {
                isInChat = false;
            } else {
                sendMessage("BROADCAST_REQ {\"message\":\"" + message + "\"}");
            }
        }

        unseenMessages.clear();
    }

    public void exit() throws IOException {
        sendMessage("BYE");
        System.exit(0);
    }

    private void processMessage(String message) throws JsonProcessingException {
        // Split the message into two parts: the type and the rest
        String[] parts = message.split(" ", 2); // Limit to 2 splits
        // parse the message type
        MessageType messageType = MessageParser.parseMessageType(parts[0]);

        switch (messageType) {
            case ENTER_RESP -> handleEnterResponse(parts[1]);

            case PING -> sendMessage("PONG");

            case BROADCAST -> handleBroadcast(parts[1]);

            case BROADCAST_RESP -> handleBroadcastResponse(parts[1]);

            case LEFT -> handleUserLeaving(parts[1]);

            case BYE_RESP -> System.out.println("Exiting the chat. See you next time!");

            case UNKNOWN_MESSAGE_TYPE -> System.out.println("Invalid message type");
        }
    }

    private void handleBroadcast(String message) throws JsonProcessingException {
        Message parsedMessage = MessageParser.parseMessage(message);

        if (!isInChat) {
            unseenMessages.add(parsedMessage);
            System.out.println("There are new messages in the chat. You have " + unseenMessages.size() + " unseen messages. Enter the chat to see the messages.");
        } else {
            System.out.println(parsedMessage.username() + ": " + parsedMessage.message() + "\n");
        }
    }

    private void handleBroadcastResponse(String message) throws JsonProcessingException {
        Status status = MessageParser.parseStatus(message);

        if (!status.isOk()) {
            System.out.println("Failed to send the message");
            switch (status.code()) {
                case 6000 -> System.out.println("User is not logged in.");
            }

        }
    }

    private void handleEnterResponse(String message) throws JsonProcessingException {
        lock.lock();

        Status status = MessageParser.parseStatus(message);

        // log the status
        // System.out.println(status);

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

    private void handleUserLeaving(String message) throws JsonProcessingException {
        Message parsedMessage = MessageParser.parseMessage(message);
        System.out.println(parsedMessage.username() + " has left the chat.");
    }



}
