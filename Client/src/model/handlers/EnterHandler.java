package model.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.MessageSender;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.receive.Status;
import model.messages.send.EnterRequest;
import utils.MessageParser;

import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class EnterHandler {
    private final MessageSender messageSender;
    private final ReentrantLock lock;
    private boolean isLoggedIn;
    private final Condition loggedIn;
    private String name;

    public EnterHandler(MessageSender messageSender) {
        this.messageSender = messageSender;
        lock = new ReentrantLock();
        loggedIn = lock.newCondition();
    }

    /**
     * Logs in the user
     * Awaits for the response from the server containing the status of the login
     *
     * @throws InterruptedException
     */
    public String logIn() throws InterruptedException, JsonProcessingException {
        lock.lock();

        if (!isLoggedIn)
            // await for the condition
            while (!isLoggedIn) {
                System.out.println("Enter your name: ");
                Scanner sc = new Scanner(System.in);
                name = sc.nextLine();

                messageSender.sendMessage(new EnterRequest(name));
                loggedIn.await();
            }
        else System.out.println("You are already logged in");


        lock.unlock();
        return this.name;
    }

    /**
     * Handles the enter response message received from the server.
     * If the user was successfully logged in, a success message is printed to the console.
     * If the user was not successfully logged in, an error message is printed to the console.
     *
     * @param payload The message received from the server
     * @throws JsonProcessingException
     */
    public void handleEnterResponse(String payload) throws JsonProcessingException {
        lock.lock();

        Status status = MessageParser.parseStatus(payload);

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

    public void handleUserJoining(String payload) throws JsonProcessingException {
        ReceivedBroadcastMessage parsedMessage = MessageParser.parseMessage(payload);
        System.out.println(parsedMessage.username() + " has joined the chat.");
    }
}
