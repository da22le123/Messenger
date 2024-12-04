package model.messages.messagefactories;

import model.messages.send.Status;

public class StatusFactory {
    public Status createEnterResponseStatus(boolean hasClient, boolean isInvalidUsername, boolean isLoggedIn, String proposedUsername) {
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

    public Status createBroadcastResponseStatus(boolean isLoggedIn) {
        if (!isLoggedIn) {
            return new Status("ERROR", 6000);
        }

        // all checks passed
        return new Status("OK", 0);
    }
}
