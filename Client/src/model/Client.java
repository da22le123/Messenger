package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.handlers.ChatHandler;
import model.handlers.EnterHandler;
import model.messages.MessageType;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.receive.Rps;
import model.messages.receive.RpsResult;
import model.messages.receive.UserlistMessage;
import model.messages.send.RpsRequest;
import model.messages.send.RpsResponse;
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
    private boolean isResponseReceived;
    private int currentStateOfGame; // 0 - no game, 1 - sender, 2 - receiver (rock-paper-scissors)
    private Condition responseReceived;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;
    private final ArrayList<String> connectedUsers;
    private final ChatHandler chatHandler;
    private final EnterHandler enterHandler;

    public Client(String ipAddress, int port) throws IOException, InterruptedException {
        this.socket = setUpSocket(ipAddress, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        lock = new ReentrantLock();
        responseReceived = lock.newCondition();
        unseenMessages = new ArrayList<>();
        connectedUsers = new ArrayList<>();
        chatHandler = new ChatHandler(out);
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
            } catch (IOException | InterruptedException e) {
                System.out.println(e.getMessage());
            }
        });
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

    public void startRpsGame() throws JsonProcessingException, InterruptedException {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the name of the user you want to play with: ");
        String opponent = sc.nextLine();

        System.out.println("Enter your choice (rock - 0, paper - 1, or scissors - 2): ");
        int choice = Integer.parseInt(sc.nextLine());

        sendMessage(new RpsRequest(opponent, choice));
        currentStateOfGame = 1; // this client is sender
    }

    public void handleRpsResult(String payload) throws JsonProcessingException {
        RpsResult rpsResult = RpsResult.fromJson(payload);
        if (!rpsResult.status().isOk()) {
            int errorCode = rpsResult.status().code();
            switch (errorCode) {
                case 3000 -> System.out.println("You are not logged in.");
                case 3001 -> System.out.println("There is already a game in progress. Users playing: " + rpsResult.nowPlaying()[0] + ", " + rpsResult.nowPlaying()[1]);
                case 3002 -> System.out.println("There is no user with the username you specified as your opponent.");
                case 3003 -> System.out.println("You cannot play with yourself.");
                case 3004 -> System.out.println("You specified an incorrect choice code.");
                case 3005 -> System.out.println("The user you specified as your opponent specified an incorrect choice code.");
            }
            return;
        }

        String opponentChoice ;

        switch (rpsResult.opponentChoice()) {
            case 0 -> opponentChoice = "rock";
            case 1 -> opponentChoice = "paper";
            case 2 -> opponentChoice = "scissors";
            default -> throw new IllegalStateException("Unexpected value: " + rpsResult.opponentChoice());
        }

        int gameResult = rpsResult.gameResult();

        // needed to switch the result of the game if the client is the receiver
        // in order to display the correct message
        if (currentStateOfGame == 2) {
            if (gameResult == 0)
                gameResult = 1;
            else if (gameResult == 1)
                gameResult = 0;
        }

        switch (gameResult) {
            case 0 -> System.out.println("You won! Opponent chose: " + opponentChoice);
            case 1 -> System.out.println("You lost! Opponent chose: " + opponentChoice);
            case 2 -> System.out.println("It's a tie!");
        }

    }

    public void handleRps(String payload) throws InterruptedException, JsonProcessingException {
        Rps rps = Rps.fromJson(payload);
        if (chatHandler.isInChat()) {
            System.out.println("You received a request to play Rock-Paper-Scissors.against " + rps.opponent() + ". Type /rps <your_choice> (rock - 0, paper - 1, or scissors - 2) in order to respond.");
        } else {
            System.out.println("You received a request to play Rock-Paper-Scissors against " + rps.opponent() + ". Enter the chat first and type /rps <your_choice> (rock - 0, paper - 1, or scissors - 2).");
        }
        chatHandler.setReceivedRps(true);
        currentStateOfGame = 2; // this client is receiver
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
    private void processMessage(String message) throws JsonProcessingException, InterruptedException {
        // Split the message into two parts: the type and the rest
        String[] parts = message.split(" ", 2); // Limit to 2 splits
        // parse the message type
        MessageType messageType = MessageParser.parseMessageType(parts[0]);

        switch (messageType) {
            case PING -> sendMessage(MessageType.PONG.toString());

            case ENTER_RESP -> enterHandler.handleEnterResponse(parts[1]);

            case BROADCAST -> chatHandler.handleBroadcast(parts[1]);

            case BROADCAST_RESP -> chatHandler.handleBroadcastResponse(parts[1]);

            case USERLIST -> handleUserlist(parts[1]);

            case DM -> chatHandler.handleDirectMessage(parts[1]);

            case DM_RESP -> chatHandler.handleDirectMessageResponse(parts[1]);

            case RPS_RESULT -> handleRpsResult(parts[1]);

            case RPS -> handleRps(parts[1]);

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
