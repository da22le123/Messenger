package model.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.MessageSender;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.receive.ReceivedDirectMessage;
import model.messages.receive.Status;
import model.messages.send.BroadcastRequest;
import model.messages.send.DmRequest;
import model.messages.send.RpsResponse;
import utils.MessageParser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChatHandler {
    private final MessageSender messageSender;
    private final PrintWriter out;
    boolean isInChat = false;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;
    private final ArrayList<ReceivedDirectMessage> unseenDirectMessages;


    private final ReentrantLock lock;
    private boolean isResponseReceived;
    private final Condition responseReceived;

    public ChatHandler(PrintWriter out) {
        this.out = out;
        unseenMessages = new ArrayList<>();
        unseenDirectMessages = new ArrayList<>();
        messageSender = new MessageSender(out);
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

        System.out.println("The chat with other users has started. Type /quitchat to exit the chat. \n " + "Type /dm <recipient> <message> to send a direct message to a user.");
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

            if (message.startsWith("/dm")) {
                String[] parts = message.split(" ", 3);
                String recipient = parts[1];
                String dm = parts[2];
                sendDirectMessage(new DmRequest(recipient, dm));
                continue;
            }

            if (message.startsWith("/rps")) {
                String[] parts = message.split(" ", 2);
                int choice = Integer.parseInt(parts[1]);
                messageSender.sendMessage(new RpsResponse(choice));
                continue;
            }

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
        ReceivedBroadcastMessage broadcastMessage = MessageParser.parseMessage(message);

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

    public void sendDirectMessage(DmRequest message) throws JsonProcessingException, InterruptedException {
        lock.lock();

        messageSender.sendMessage(message);

        while (!isResponseReceived) {
            responseReceived.await();
        }
        lock.unlock();
    }

    public boolean isInChat() {
        return isInChat;
    }


}
