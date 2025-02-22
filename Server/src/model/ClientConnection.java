package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.messages.MessageType;
import model.messages.messagefactories.StatusFactory;
import model.messages.receive.*;
import model.messages.send.*;
import utils.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ClientConnection {
    private String username;
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    private ClientManager clientManager;
    private FileTransferManager fileTransferManager;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition pongReceived = lock.newCondition();
    private volatile boolean isPongReceived = false;
    private volatile boolean isPingSent = false;


    public ClientConnection(Socket socket, ClientManager clientManager, FileTransferManager fileTransferManager) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.clientManager = clientManager;
        this.fileTransferManager = fileTransferManager;
    }

    public void startMessageProcessingThread() {
        new Thread(() -> {
            try {
                sendMessage(new ReadyMessage("1.6.0"));

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    System.out.println("Received message: " + clientMessage);
                    processMessage(clientMessage);
                }

                cleanUp();
            } catch (IOException e) {
                cleanUp();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void processMessage(String clientMessage) throws IOException, InterruptedException {
        // Split the message into two parts: the type and the rest
        String[] parts = clientMessage.split(" ", 2); // Limit to 2 splits
        // parse the message type
        MessageType messageType = MessageParser.parseMessageType(parts[0]);
        // get the payload
        String payload = parts.length > 1 ? parts[1] : null;

        switch (messageType) {
            case ENTER -> handleEnter(payload);
            case PONG -> handlePong();
            case BROADCAST_REQ -> handleBroadcast(payload);
            case USERLIST_REQ -> handleUserlist();
            case DM_REQ -> handleDmRequest(payload);
            case RPS_REQ -> handleRpsRequest(payload);
            case RPS_RESP -> handleRpsResponse(payload);
            case FILE_REQ -> handleFileRequest(payload);
            case FILE_RESP -> handleFileResponse(payload);
            case TTT_REQ -> handleTttRequest(payload);
            case TTT_RESP -> handleTttResponse(payload);
            case TTT_MOVE -> handleTttMove(payload);
            case BYE -> handleBye();
        }
    }

    private void handleFileRequest(String payload) throws JsonProcessingException {
        FileRequest fileRequest = FileRequest.fromJson(payload);

        StatusFactory statusFactory = new StatusFactory(clientManager);
        Status status = statusFactory.createFileRequestResponseStatus(this.username, fileRequest.recipient());

        if (!status.isOk()) {
            sendMessage(new Response(MessageType.FILE_RESP, status));
            return;
        }

        ClientConnection recipient = clientManager.getClientByUsername(fileRequest.recipient());
        // send a "File" message to the recipient in order to get acceptance.
        recipient.sendMessage(new File(this.username, fileRequest.filename(), fileRequest.hash()));
        // set client that sent a request as awaitingAcceptance
        clientManager.addAwaitingAcceptanceClient(this);
    }

    private void handleFileResponse(String payload) throws JsonProcessingException {
        FileResponseReceive fileResponse = FileResponseReceive.fromJson(payload);

        // if "sender" that the client specified on the FILE_RESP did not request a file transfer
        if (!clientManager.isAwaitingAcceptance(fileResponse.sender())) {
            sendMessage(new Response(MessageType.FILE_RESP, new Status("ERROR", 9004)));
            return;
        }

        // send back the status to the client that requested the file
        ClientConnection sender = clientManager.getClientByUsername(fileResponse.sender());
        sender.sendMessage(new FileResponseSend(this.username, fileResponse.status()));

        if (fileResponse.status().isOk()) {
            // send UUIDs to both clients
            String uuid = UUID.randomUUID().toString();
            // to sender
            sender.sendMessage(new FileUUID(uuid));
            // to recipient
            sendMessage(new FileUUID(uuid));
            // add the transfer to the map
            fileTransferManager.addTransfer(uuid);
        }
    }

    private void handleRpsRequest(String payload) throws JsonProcessingException, InterruptedException {
        RpsRequest rpsRequest = RpsRequest.fromJson(payload);

        StatusFactory statusFactory = new StatusFactory(clientManager);
        Status status = statusFactory.createRpsResultStatus(this.username, rpsRequest.opponent(), rpsRequest.choice());
        RpsResult rpsResult = new RpsResult(status);
        if (!rpsResult.getStatus().isOk()) {
            if (rpsResult.getStatus().code()==3001) {
                rpsResult.setNowPlaying(clientManager.getNowPlayingRps());
            }

            sendMessage(rpsResult);
            return;
        }


        ClientConnection opponent = clientManager.getClientByUsername(rpsRequest.opponent());
        opponent.sendMessage(new Rps(this.username));
        // start a game
        clientManager.startRpsGame(this.username, rpsRequest.opponent(), rpsRequest.choice());
    }

    private void handleRpsResponse(String payload) throws JsonProcessingException {
        RpsResponse rpsResponse = RpsResponse.fromJson(payload);
        clientManager.addPlayer2Choice(rpsResponse.choice());
        StatusFactory statusFactory = new StatusFactory(clientManager);
        Status status = statusFactory.createRpsResultStatus(rpsResponse.choice());

        if (!status.isOk()) {
            clientManager.sendRpsResultToPlayers(status);
            clientManager.abortRpsGame();
            return;
        }

        clientManager.calculateGameResult();
        clientManager.sendRpsResultToPlayers(status);
        clientManager.abortRpsGame();
    }

    private void handleDmRequest(String payload) throws JsonProcessingException {
        DmRequest dmRequest = DmRequest.fromJson(payload);

        Status status = new StatusFactory(clientManager).createDmResponseStatus(this.username, dmRequest.recipient());

        // send a response to the client that requested the DM
        if (status != null) {
            sendMessage(new Response(MessageType.DM_RESP, status));
        } else {
            throw new RuntimeException("No response was created by the factory.");
        }

        // send the DM to the client that is supposed to receive it
        if (status.isOk()) {
            ClientConnection recipient = clientManager.getClientByUsername(dmRequest.recipient());
            recipient.sendMessage(new DirectMessage(this.username, dmRequest.message()));
        }
    }

    private void handleTttRequest(String payload) throws JsonProcessingException {
        TttRequestReceive tttRequest = TttRequestReceive.fromJson(payload);

        Status status = new StatusFactory(clientManager).createTttResponseStatus(this.username, tttRequest.opponent(), tttRequest.board());


        if (!status.isOk()) {
            if (status.code()==2001) {
                TttResult nowPlayingResult = TttResult.fromStatusNNowPlaying(status, clientManager.getNowPlayingTtt());
                sendMessage(nowPlayingResult);
            } else {
                sendMessage(TttResult.fromStatus(status));
            }
            return;
        }

        ClientConnection opponent = clientManager.getClientByUsername(tttRequest.opponent());

        clientManager.startTttGame(this, opponent);
        clientManager.addTttMove(tttRequest.board());

        opponent.sendMessage(new TttRequestSend(this.username, clientManager.getCurrentTttGame().getBoard()));
    }

    private void handleTttResponse(String payload) throws IOException {
        TttResponse tttResponse = TttResponse.fromJson(payload);

        if (clientManager.getCurrentTttGame()==null || clientManager.getCurrentTttGame().getNumberOfMovesOnTheBoard() != 1) {
            sendMessage(TttResult.fromStatus(new Status("ERROR", 2007)));
            return;
        }

        // if ok, add C2 as the next player to make a move
        if (tttResponse.status().isOk()) {
            clientManager.setNextPlayerToMove(this);
        }
        // if not ok, send the result to the first player, notifying that the request has been rejected
        else if (tttResponse.status().code()==2005) {
            Status status = new Status(tttResponse.status().status(), tttResponse.status().code());
            TttResult result = TttResult.fromStatus(status);
            clientManager.getCurrentTttGame().getPlayer1().sendMessage(result);
            clientManager.abortTttGame();
        }
    }

    private void handleTttMove(String payload) throws JsonProcessingException {
        TttMove tttMove = TttMove.fromJson(payload);

        Status status = new StatusFactory(clientManager).createTttMoveStatus(this, tttMove.board());
        // get the opponent
        ClientConnection opponent = clientManager.getTttOpponent(this);

        // if the move is valid, add it to the board and check if the game has ended, if not,
        // swap the next player to move and send the move response
        if (status.isOk()) {
            clientManager.addTttMove(tttMove.board());
            int gameResult = clientManager.getTttResult();

            // if the game has ended, send the result to both players
            if (gameResult != -1) {
                sendMessage(TttResult.fromStatusNGameResultNBoard(new Status("OK", 0), gameResult, clientManager.getCurrentTttGame().getBoard()));
                opponent.sendMessage(TttResult.fromStatusNGameResultNBoard(new Status("OK", 0), gameResult, clientManager.getCurrentTttGame().getBoard()));
                clientManager.abortTttGame();
                return;
            }

            clientManager.swapNextPlayerToMove();
        }

        //respond to the move
        sendMessage(new TttMoveResponse(status, clientManager.getCurrentTttGame().getBoard()));


        // send the move to the opponent if the move was valid
        if (status.isOk()) {
            opponent.sendMessage(new Ttt(clientManager.getCurrentTttGame().getBoard()));
        }
    }











    private void handleUserlist() {
        List<String> usernames =  clientManager.getClients().stream()
                .map(ClientConnection::getUsername)
                .toList();

        sendMessage(new UserlistMessage(usernames));
    }

    private void handleBroadcast(String payload) throws JsonProcessingException {
        BroadcastRequest broadcastRequest = BroadcastRequest.fromJson(payload);

        StatusFactory statusFactory = new StatusFactory(clientManager);
        Status status = statusFactory.createBroadcastResponseStatus(this.username);

        if (status != null) {
            sendMessage(new Response(MessageType.BROADCAST_RESP, status));
        } else {
            throw new RuntimeException("No response was created by the factory.");
        }

        if (status.isOk()) {
            clientManager.sendMessageToAllClients(new BroadcastMessage(this.username, broadcastRequest.message()), this);
        }
    }

    private void handleBye() throws JsonProcessingException, InterruptedException {
        sendMessage(new Response(MessageType.BYE_RESP, new Status("OK", 0)));
        clientManager.sendMessageToAllClients(new LeftMessage(this.username), this);

        clientManager.removeClient(this);
        scheduler.shutdownNow();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);

        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void cleanUp() {
        if (clientManager.isPlayingRpsNow(this)) {
            clientManager.abortRpsGame();
        }
        if (clientManager.isPlayingTttNow(this)) {
            clientManager.abortTttGame();
        }
        clientManager.removeClient(this);
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePong() {
        lock.lock();
        try {
            if (isPingSent) {
                isPongReceived = true;
                pongReceived.signal();
            } else {
                sendMessage(new PongErrorMessage(8000));
            }
        } finally {
            lock.unlock();
        }
    }

    public void pingPong() {
        lock.lock();

        while (!isPongReceived) {
            sendMessage("PING");
            isPingSent = true;
            try {
                // hangup if we do not receive pong in 3 seconds
                if (!pongReceived.await(3, TimeUnit.SECONDS)) {
                    hangUp();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        isPingSent = false;
        isPongReceived = false;
        lock.unlock();
    }

    public void startPingScheduler() {
        scheduler.scheduleAtFixedRate(this::pingPong, 10, 10, TimeUnit.SECONDS);
    }


    private void hangUp() throws InterruptedException, JsonProcessingException {
        sendMessage(new HangupMessage(7000));
        clientManager.sendMessageToAllClients(new LeftMessage(this.username), this);

        cleanUp();
    }

    private void handleEnter(String payload) throws JsonProcessingException {
        EnterRequest enterRequest = EnterRequest.fromJson(payload);

        if (enterRequest == null) {
            sendMessage(MessageType.PARSE_ERROR.toString());
            return;
        }

        String proposedUsername = enterRequest.username();
        StatusFactory statusFactory = new StatusFactory(clientManager);
        Status status = statusFactory.createEnterResponseStatus(this.username, proposedUsername);

        if (status != null) {
            sendMessage(new Response(MessageType.ENTER_RESP, status));
        } else {
            throw new RuntimeException("No response was created by the factory.");
        }

        if (status.isOk()) {
            // set the current username
            this.username = proposedUsername;
            // add the client to the list of clients
            clientManager.addClient(this);
            // notify all clients that a new client has entered
            clientManager.sendMessageToAllClients(new JoinedMessage(this.username), this);

        }

        startPingScheduler();
    }

    private void sendMessage(String message) {
        System.out.println("Sending message: " + message);
        out.println(message);
    }

    private void sendMessage(Sendable message) {
        try {
            sendMessage(message.toJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getPrintWriter() {
        return out;
    }

    public BufferedReader getBufferedReader() {
        return in;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getUsername() != null && ((ClientConnection) obj).getUsername() != null) {
            return this.getUsername().equals(((ClientConnection) obj).getUsername());
        }

        return this.getSocket().equals(((ClientConnection) obj).getSocket());
    }
}
