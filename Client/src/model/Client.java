package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.handlers.ChatHandler;
import model.handlers.EnterHandler;
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
    private Socket fileTransferSocket;
    private PrintWriter fileOut;
    private BufferedReader fileIn;
    private String name;

    // hash of the file that is being transferred
    private String currentFileTransferHash;
    // 0 - no file transfer, 1 - this instance is the sender, 2 - this instance is receiver
    private int currentFileTransferStatus;
    // path of the file on the machine of sender
    private String filePathSending;
    // name of the file + extension
    private String fileName;

    private final ReentrantLock lock;
    private boolean isResponseReceived;
    private Condition responseReceived;
    private final ArrayList<ReceivedBroadcastMessage> unseenMessages;
    private final ArrayList<String> connectedUsers;
    private final ChatHandler chatHandler;
    private final EnterHandler enterHandler;
    private final RpsHandler rpsHandler;

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
        currentFileTransferStatus = 0;
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

            case FILE -> handleFile(payload);

            case FILE_RESP -> handleFileResponse(payload);

            case FILE_UUID -> handleFileUUID(payload);

            case JOINED -> enterHandler.handleUserJoining(payload);

            case LEFT -> handleUserLeaving(payload);

            case BYE_RESP -> handleByeResponse();

            case PARSE_ERROR -> System.out.println("Parse error");

            case UNKNOWN_COMMAND -> System.out.println("Unknown command");
        }
    }

    private void handleFile(String payload) throws JsonProcessingException {
        File fileMessage = File.fromJson(payload);
        currentFileTransferStatus = 2;
        chatHandler.addIncomingFileRequest(fileMessage.sender());
        currentFileTransferHash = fileMessage.hash();
        fileName = fileMessage.filename();

        if (chatHandler.isInChat()) {
            System.out.println("You have received a file from " + fileMessage.sender() + ". The file name is " + fileName + ". Do you want to accept it? Type /file_answer (yes/no)");
        } else {
            System.out.println("You have received a file from " + fileMessage.sender() + ". The file name is " + fileName + ". To answer, you have to enter the chat first.");
        }
    }

    private void handleFileResponse(String payload) throws IOException {
        FileResponseReceive fileResponse = FileResponseReceive.fromJson(payload);


        if (fileResponse.status().isOk()) {
            System.out.println("The file transfer has been accepted by recipient.");
        } else {
            int errorCode = fileResponse.status().code();

            switch(errorCode) {
                case 9000:
                    System.out.println("You are not logged in.");
                    break;
                case 9001:
                    System.out.println("You specified non-existent recipient's username.");
                    break;
                case 9002:
                    System.out.println("You cannot send a file to yourself.");
                    break;
                case 9003:
                    System.out.println("The recipient declined the file transfer.");
                    break;
                default:
                    System.out.println("Unknown error.");
            }

            currentFileTransferStatus = 0;
        }
    }

    private void handleFileUUID(String payload) throws IOException, NoSuchAlgorithmException {
        FileUUID fileUUID = FileUUID.fromJson(payload);
        System.out.println("File UUID: " + fileUUID.uuid() + " Sending it to the server on 1338 port.");
        fileTransferSocket = setUpSocket(ipAddress, 1338);
        System.out.println("File transfer socket established.");
        fileOut = new PrintWriter(fileTransferSocket.getOutputStream(), true);

        String mode = currentFileTransferStatus == 1 ? "_send" : "_receive";

        fileOut.println(fileUUID.uuid() + mode);
        System.out.println("sent " + fileUUID.uuid() + mode + " to server");
        switch (currentFileTransferStatus) {
            case 1 -> new Thread(() -> sendFile(filePathSending, fileTransferSocket)).start();
            case 2 -> new Thread(this::receiveFile).start();
        }
    }

    public void sendFile(String filePath, Socket fileTransferSocket) {
        System.out.println("Sending file...");
        try (FileInputStream in = new FileInputStream(new java.io.File(filePath));
             OutputStream out = fileTransferSocket.getOutputStream()) {


            // transfer the file directly to the output stream
            in.transferTo(out);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        cleanUpStateOfFileTransfer();
    }

    private void receiveFile() {
        System.out.println("Receiving file...");
        try (InputStream in = fileTransferSocket.getInputStream();
             FileOutputStream fileOut = new FileOutputStream("/Users/illiapavelko/" + fileName)) {


            // Copy the entire stream directly into the file
            in.transferTo(fileOut);

            String receivedFileChecksum = CheckSumCalculator.calculateSHA256("/Users/illiapavelko/" + fileName);
            if (currentFileTransferHash.equals(receivedFileChecksum)) {
                System.out.println("File received successfully, the checksum of the file is the same as before sending it.");
            } else {
                System.out.println("File received, but the checksums do not match.");
            }
            // System.out.println("123 file checksum: " + receivedFileChecksum);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        cleanUpStateOfFileTransfer();
    }

    private void cleanUpStateOfFileTransfer() {
        currentFileTransferHash = null;
        currentFileTransferStatus = 0;
        filePathSending = null;
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
        filePathSending = sc.nextLine();

        String fileExtension = filePathSending.substring(filePathSending.lastIndexOf(".") + 1);

        System.out.println("Enter the name of the file. Please, don't include the file extension: ");
        fileName = sc.nextLine() + "." + fileExtension;

        String checksum;
        try {
            checksum = CheckSumCalculator.calculateSHA256(filePathSending);
        } catch (IOException e) {
            System.out.println("File not found.");
            return;
        }

        sendMessage(new FileRequest(receiverUsername, fileName, checksum));
        currentFileTransferStatus = 1;
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
