package model.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.MessageSender;
import model.messages.Status;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.receive.ReceivedDirectMessage;
import model.messages.send.*;
import utils.MessageParser;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChatHandler {
    private static final String DESCRIPTION =
            "The chat with other users has started.\n" +
            "Type /quitchat to exit the chat.\n" +
            "Type /dm <recipient> <message> to send a direct message to a user.\n" +
            "Type /rps <choice> to play rock-paper-scissors to respond the incoming game request.\n" +
            "Type /file_requests to view all senders that sent a file request.\n" +
            "Type /file_answer <sender> <yes/no> to answer to the incoming file transfer request.\n" +
            "Type /ttt_answer <yes/no> to respond to the incoming game request.\n" +
            "Type /ttt_move <move> to make a move in the ttt game.\n" +
            "Type /help to see all available commands again.\n";

    private final MessageSender messageSender;
    private boolean isInChat = false;
    private boolean isReceivedRps = false;
    private boolean isReceivedTtt = false;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;
    private final ArrayList<ReceivedDirectMessage> unseenDirectMessages;
    private final ArrayList<String> incomingFileRequests;


    private final ReentrantLock lock;
    private boolean isResponseReceived;
    private final Condition responseReceived;

    private String[] currentTttBoard;
    private boolean isPlayer1;


    public ChatHandler(MessageSender messageSender) {
        unseenMessages = new ArrayList<>();
        unseenDirectMessages = new ArrayList<>();
        incomingFileRequests = new ArrayList<>();
        this.messageSender = messageSender;
        lock = new ReentrantLock();
        responseReceived = lock.newCondition();
    }

    /**
     * Starts the chat with other users, first prints the unseen messages if there are any. Then awaits for the user input
     * If user types /quitchat, the chat ends
     * If user types /dm recipient message, a direct message is sent to the recipient
     */
    public void startChatting() throws JsonProcessingException, InterruptedException {
        Scanner sc = new Scanner(System.in);

        System.out.println(DESCRIPTION);

        isInChat = true;

        if (!unseenDirectMessages.isEmpty()) {
            System.out.println("Unseen direct messages: ");
            for (ReceivedDirectMessage m : unseenDirectMessages) {
                System.out.println("DM from " + m.sender() + ": " + m.message() + "\n");
            }
        }

        if (!unseenMessages.isEmpty()) {
            System.out.println("Unseen messages: ");
            for (ReceivedBroadcastMessage m : unseenMessages) {
                System.out.println(m.username() + ": " + m.message() + "\n");
            }
        }

        while (isInChat) {
            // not a busy wait because it blocks the thread execution until the user enters a message
            String message = sc.nextLine();

            /*
            todo extract the following commands to separate methods
             */

            // send a dm
            if (message.startsWith("/dm")) {
                dmCommand(message);
            } else

                // answer to the incoming rps request
                if (message.startsWith("/rps")) {
                    rpsCommand(message);
                } else

                    // quit the chat
                    if (message.equals("/quitchat")) {
                        isInChat = false;
                    } else

                        // answer to the incoming ttt game request
                        if (message.startsWith("/ttt_answer")) {
                            tttAnswerCommand(message);
                        } else
                            // make a move in the ttt game
                            if (message.startsWith("/ttt_move")) {
                                tttMoveCommand(message);
                            } else

                                // view all incoming file requests
                                if (message.equals("/file_requests")) {
                                    fileRequestsCommand();
                                } else

                                    // answer to the incoming file transfer request     // view description
                                    if (message.startsWith("/file_answer")) {
                                        fileAnswerCommand(message);
                                    } else if (message.equals("/help")) {
                                        System.out.println(DESCRIPTION);
                                    } else {
                                        messageSender.sendMessage(new BroadcastRequest(message));
                                    }
        }

        unseenMessages.clear();
    }

    private void fileAnswerCommand(String message) throws JsonProcessingException {
        if (incomingFileRequests.isEmpty()) {
            System.out.println("You have no incoming file transfer requests.");
            return;
        }

        String[] parts = message.split(" ", 3);

        if (parts.length != 3) {
            System.out.println("Invalid command. Please type '/file_answer <sender> <yes/no>'");
            return;
        }

        String sender = parts[1];
        String answer = parts[2];

        if (!hasIncomingFileRequestFromSender(sender)) {
            System.out.println("You have no incoming file transfer requests from " + sender);
            return;
        }

        if (answer.equals("yes")) {
            System.out.println("You accepted the file transfer.");
            messageSender.sendMessage(new FileResponseSend(sender, new Status("OK", 0)));

        } else if (answer.equals("no")) {
            System.out.println("You declined the file transfer.");
            messageSender.sendMessage(new FileResponseSend(sender, new Status("ERROR", 9003)));
        } else {
            System.out.println("Invalid choice. Please type 'yes' or 'no'.");
        }
    }

    private void fileRequestsCommand() {
        if (incomingFileRequests.isEmpty()) {
            System.out.println("You have no incoming file transfer requests.");
            return;
        }

        System.out.println("Incoming file transfer requests from: ");
        for (String sender : incomingFileRequests) {
            System.out.println(sender);
        }
    }

    private void rpsCommand(String message) throws JsonProcessingException {
        if (!isReceivedRps) {
            System.out.println("You have no incoming game requests.");
            return;
        }

        String[] parts = message.split(" ", 2);
        int choice = Integer.parseInt(parts[1]);
        messageSender.sendMessage(new RpsResponse(choice));
        setReceivedRps(false);
    }

    private void tttAnswerCommand(String message) throws JsonProcessingException {
        if (!isReceivedTtt) {
            System.out.println("You have no incoming game requests.");
            return;
        }

        String[] parts = message.split(" ", 2);
        String answer = parts[1];

        switch (answer.toLowerCase(Locale.ROOT)) {
            case "yes":
                messageSender.sendMessage(new TttResponse(new Status("OK", 0)));
                System.out.println("You have accepted the game request. Your opponent has already made the first move (see the current board above).");
                System.out.println("Please, enter your move via /ttt_move <move> command.");
                break;
            case "no":
                messageSender.sendMessage(new TttResponse(new Status("ERROR", 2005)));
                setReceivedTtt(false);
                //reset the board
                setCurrentTttBoard(new String[]{" ", " ", " ", " ", " ", " ", " ", " ", " "});
                System.out.println("You have declined the game request.");
                break;
            default:
                System.out.println("Invalid choice. Please type 'yes' or 'no'.");
        }
    }

    private void tttMoveCommand(String message) throws JsonProcessingException {
        String[] parts = message.split(" ", 2);
        if (parts.length != 2) {
            System.out.println("Invalid command. Please type '/ttt_move <move>'");
            return;
        }

        int move = Integer.parseInt(message.split(" ", 2)[1].trim());
        String[] newBoard = TttHandler.applyMove(currentTttBoard, move, isPlayer1);
        messageSender.sendMessage(new TttMove(newBoard));
        System.out.println("Your move has been sent.");
    }

    private void dmCommand(String message) throws JsonProcessingException, InterruptedException {
        String[] parts = message.split(" ", 3);
        String recipient = parts[1];
        String dm = parts[2];
        sendDirectMessage(new DmRequest(recipient, dm));
    }

    /**
     * Handles the broadcast response message received from the server.
     * If the message was not sent successfully, an error message is printed to the console.
     *
     * @param payload The message received from the server
     * @throws JsonProcessingException
     */
    public void handleBroadcastResponse(String payload) throws JsonProcessingException {
        Status status = MessageParser.parseStatus(payload);

        if (!status.isOk()) {
            System.out.println("Failed to send the message");
            switch (status.code()) {
                case 6000 -> System.out.println("User is not logged in.");
            }
        }
    }

    /**
     * Handles the broadcast message received from the server.
     * If the user is not in the chat, the message is added to the unseen messages list.
     * If the user is in the chat, the message is printed to the console.
     *
     * @param payload The message received from the server
     * @throws JsonProcessingException
     */
    public void handleBroadcast(String payload) throws JsonProcessingException {
        ReceivedBroadcastMessage broadcastMessage = MessageParser.parseMessage(payload);

        if (!isInChat) {
            unseenMessages.add(broadcastMessage);
            System.out.println("There are new messages in the chat. You have " + unseenMessages.size() + " unseen messages. Enter the chat to see the messages.");
        } else {
            System.out.println(broadcastMessage.username() + ": " + broadcastMessage.message() + "\n");
        }
    }

    public void handleDirectMessage(String payload) throws JsonProcessingException {
        ReceivedDirectMessage directMessage = ReceivedDirectMessage.fromJson(payload);

        if (!isInChat) {
            unseenDirectMessages.add(directMessage);
            System.out.println("There are new direct messages for you. You have " + unseenDirectMessages.size() + " unseen direct messages. Enter the chat to see direct messages.");
        } else {
            System.out.println("DM from " + directMessage.sender() + ": " + directMessage.message());
        }
    }

    public void handleDirectMessageResponse(String payload) throws JsonProcessingException {
        lock.lock();
        Status status = Status.fromJson(payload);

        if (status.isOk()) {
            System.out.println("Message sent successfully.");
        } else if (status.code() == 4000) {
            System.out.println("You are not logged in.");
        } else if (status.code() == 4001) {
            System.out.println("The recipient is not not found.");
        }

        isResponseReceived = true;
        responseReceived.signal();

        lock.unlock();
    }

    public void sendDirectMessage(DmRequest payload) throws JsonProcessingException, InterruptedException {
        lock.lock();

        messageSender.sendMessage(payload);

        while (!isResponseReceived) {
            responseReceived.await();
        }
        lock.unlock();
    }

    public boolean isInChat() {
        return isInChat;
    }

    public void setReceivedRps(boolean receivedRps) {
        isReceivedRps = receivedRps;
    }

    public void setReceivedTtt(boolean receivedTtt) {
        isReceivedTtt = receivedTtt;
    }

    public void addIncomingFileRequest(String sender) {
        incomingFileRequests.add(sender);
    }

    private boolean hasIncomingFileRequestFromSender(String sender) {
        for (String senderName : incomingFileRequests) {
            if (senderName.equals(sender)) {
                return true;
            }
        }
        return false;
    }

    public String[] getCurrentTttBoard() {
        return currentTttBoard;
    }

    public void setCurrentTttBoard(String[] currentTttBoard) {
        this.currentTttBoard = currentTttBoard;
    }

    public boolean isPlayer1() {
        return isPlayer1;
    }

    public void setIsPlayer1(boolean isPlayer1) {
        this.isPlayer1 = isPlayer1;
    }





}
