package model.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.MessageSender;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.receive.Status;
import model.messages.send.BroadcastRequest;
import utils.MessageParser;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class BroadcastHandler {
    private final MessageSender messageSender;
    private final PrintWriter out;
    boolean isInChat = false;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;
    public BroadcastHandler(PrintWriter out) {
        this.out = out;
        unseenMessages = new ArrayList<>();
        messageSender = new MessageSender(out);
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
            // not a busy wait because it blocks the thread execution until the user enters a message
            String message = sc.nextLine();

            if (message.equals("/quitchat")) {
                isInChat = false;
            } else {
                messageSender.sendMessage(new BroadcastRequest(message));
            }
        }

        unseenMessages.clear();
    }

    /**
     * Handles the broadcast response message received from the server.
     * If the message was not sent successfully, an error message is printed to the console.
     *
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    public void handleBroadcastResponse(String message) throws JsonProcessingException {
        Status status = MessageParser.parseStatus(message);

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
     * @param message The message received from the server
     * @throws JsonProcessingException
     */
    public void handleBroadcast(String message) throws JsonProcessingException {
        ReceivedBroadcastMessage parsedMessage = MessageParser.parseMessage(message);

        if (!isInChat) {
            unseenMessages.add(parsedMessage);
            System.out.println("There are new messages in the chat. You have " + unseenMessages.size() + " unseen messages. Enter the chat to see the messages.");
        } else {
            System.out.println(parsedMessage.username() + ": " + parsedMessage.message() + "\n");
        }
    }
}
