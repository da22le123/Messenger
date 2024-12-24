package model.messages.messagefactories;

import model.ClientConnection;
import model.ClientManager;
import model.messages.send.Status;
import utils.ValidationUtils;

public class StatusFactory {
    private ClientManager clientManager;
    public StatusFactory(ClientManager clientManager) {
        this.clientManager = clientManager;
    }
    public Status createEnterResponseStatus(String currentUsername, String proposedUsername) {
        boolean isInvalidUsername = proposedUsername.isEmpty() || !ValidationUtils.isValidUsername(proposedUsername);
        boolean hasClient = clientManager.hasClient(proposedUsername);
        boolean isLoggedIn = currentUsername != null;

        // already has a client with the same username
        if (hasClient) {
            return new Status("ERROR", 5000);
        }

        // invalid username
        if (isInvalidUsername) {
            return new Status("ERROR", 5001);
        }

        // already logged in
        if (isLoggedIn) {
            return new Status("ERROR", 5002);
        }

        if (!(hasClient && isInvalidUsername && isLoggedIn)) {
            // all checks passed
            return new Status("OK", 0);
        }

        return null;
    }

    public Status createBroadcastResponseStatus(String currentUsername) {
        if (currentUsername==null) {
            return new Status("ERROR", 6000);
        }

        // all checks passed
        return new Status("OK", 0);
    }

    public Status createDmResponseStatus(String currentUsername, String recipientUsername) {
        boolean isLoggedIn = currentUsername!=null;
        boolean recipientExists = clientManager.hasClient(recipientUsername);

        if (!isLoggedIn) {
            return new Status("ERROR", 4000);
        }

        if (!recipientExists) {
            return new Status("ERROR", 4001);
        }

        // all checks passed
        return new Status("OK", 0);
    }

    /**
     *  Create a status object for the result of the RPS game
     *  based on the choice of the client that sends the RPS request
     * @param C1 Username of the client that sends the RPS request
     * @param C2 Username of the client that receives the RPS request
     * @param choice
     * @return
     */
    public Status createRpsResultStatus(String C1, String C2, int choice) {
        ClientConnection client1 = clientManager.getClientByUsername(C1);
        ClientConnection client2 = clientManager.getClientByUsername(C2);
        if (client1 == null) {
            return new Status("ERROR", 3000);
        }

        if (clientManager.IsRpsGameRunning()) {
            return new Status("ERROR", 3001);
        }

        if (client2 == null) {
            return new Status("ERROR", 3002);
        }

        if (client1.equals(client2)) {
            return new Status("ERROR", 3003);
        }

        if (choice < 0 || choice > 2) {
            return new Status("ERROR", 3004);
        }

        // all checks passed
        return new Status("OK", 0);
    }


    /**
     * Create a status object for the result of the RPS game
     * based on choice of the client that receives the RPS request
     * @param opponentChoice
     * @return
     */
    public Status createRpsResultStatus(int opponentChoice) {
        if (opponentChoice < 0 || opponentChoice > 2) {
            return new Status("ERROR", 3005);
        }

        // all checks passed
        return new Status("OK", 0);
    }

}
