package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.handlers.ChatHandler;
import model.handlers.EnterHandler;
import model.handlers.FileTransferHandler;
import model.handlers.RpsHandler;
import model.messages.MessageType;
import model.messages.receive.*;
import model.messages.receive.File;
import model.messages.send.FileRequest;
import model.messages.send.RpsRequest;
import model.messages.send.Sendable;
import utils.CheckSumCalculator;
import utils.MessageParser;

import java.awt.print.PrinterException;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private final String ipAddress;
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private String name;



    private final ReentrantLock lock;
    private boolean isResponseReceived;
    private Condition responseReceived;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;
    private final ArrayList<String> connectedUsers;

    // Handlers
    private final ChatHandler chatHandler;
    private final EnterHandler enterHandler;
    private final RpsHandler rpsHandler;
    private final FileTransferHandler fileTransferHandler;


    public Client(String ipAddress, int port) throws IOException, InterruptedException {
        this.socket = setUpSocket(ipAddress, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.ipAddress = ipAddress;
        lock = new ReentrantLock();
        responseReceived = lock.newCondition();
        unseenMessages = new ArrayList<>();
        connectedUsers = new ArrayList<>();
        chatHandler = new ChatHandler(out);
        enterHandler = new EnterHandler(out);
        rpsHandler = new RpsHandler(chatHandler);
        fileTransferHandler = new FileTransferHandler(chatHandler, ipAddress);
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
            } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Processes the message received from the server based on the message type
     *
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    private void processMessage(String message) throws IOException, InterruptedException, NoSuchAlgorithmException {
        // Split the message into two parts: the type and the rest
        String[] parts = message.split(" ", 2); // Limit to 2 splits
        // parse the message type
        MessageType messageType = MessageParser.parseMessageType(parts[0]);
        // get the payload of the message
        String payload = parts.length > 1 ? parts[1] : null;

        switch (messageType) {
            case PING -> sendMessage(MessageType.PONG.toString());

            case ENTER_RESP -> enterHandler.handleEnterResponse(payload);

            case BROADCAST -> chatHandler.handleBroadcast(payload);

            case BROADCAST_RESP -> chatHandler.handleBroadcastResponse(payload);

            case USERLIST -> handleUserlist(payload);

            case DM -> chatHandler.handleDirectMessage(payload);

            case DM_RESP -> chatHandler.handleDirectMessageResponse(payload);

            case RPS_RESULT -> rpsHandler.handleRpsResult(payload);

            case RPS -> rpsHandler.handleRps(payload);

            case FILE -> fileTransferHandler.handleFile(payload);

            case FILE_RESP -> fileTransferHandler.handleFileResponse(payload);

            case FILE_UUID -> fileTransferHandler.handleFileUUID(payload);

            case JOINED -> enterHandler.handleUserJoining(payload);

            case LEFT -> handleUserLeaving(payload);

            case BYE_RESP -> handleByeResponse();

            case PARSE_ERROR -> System.out.println("Parse error");

            case UNKNOWN_COMMAND -> System.out.println("Unknown command");
        }
    }

    public void logIn() throws InterruptedException, JsonProcessingException {
        enterHandler.logIn();
    }

    public void startChatting() throws JsonProcessingException, InterruptedException {
        chatHandler.startChatting();
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

    public void startRpsGame() throws JsonProcessingException {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the name of the user you want to play with: ");
        String opponent = sc.nextLine();

        System.out.println("Enter your choice (rock - 0, paper - 1, or scissors - 2): ");
        int choice = Integer.parseInt(sc.nextLine());

        sendMessage(new RpsRequest(opponent, choice));
    }

    public void sendFile() throws NoSuchAlgorithmException, JsonProcessingException {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the name of the user you want to send the file to: ");
        String receiverUsername = sc.nextLine();

        System.out.println("Enter the path of the file you want to send: ");
        fileTransferHandler.setFilePathSending(sc.nextLine());

        String filePathSending = fileTransferHandler.getFilePathSending();

        String fileExtension = filePathSending.substring(filePathSending.lastIndexOf(".") + 1);

        System.out.println("Enter the name of the file. Please, don't include the file extension: ");
        fileTransferHandler.setFileName(sc.nextLine() + "." + fileExtension);

        String checksum;
        try {
            checksum = CheckSumCalculator.calculateSHA256(filePathSending);
        } catch (IOException e) {
            System.out.println("File not found.");
            return;
        }

        sendMessage(new FileRequest(receiverUsername, fileTransferHandler.getFileName(), checksum));
        fileTransferHandler.setCurrentFileTransferStatus(1);
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

}
