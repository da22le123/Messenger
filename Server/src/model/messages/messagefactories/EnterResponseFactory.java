package model.messages.messagefactories;

import model.messages.MessageType;
import model.messages.send.Response;
import model.messages.send.Status;
import utils.ValidationUtils;

public class EnterResponseFactory {
    public Response createEnterResponse(boolean hasClient, boolean isInvalidUsername, boolean isLoggedIn, String proposedUsername) {
        // already has a client with the same username
        if (hasClient) {
            return new Response(MessageType.ENTER_RESP, new Status("ERROR", 5000));
        }

        // invalid username
        if (isInvalidUsername) {
            return new Response(MessageType.ENTER_RESP, new Status("ERROR", 5001));
        }

        // already logged in
        if (isLoggedIn) {
            return new Response(MessageType.ENTER_RESP, new Status("ERROR", 5002));
        }

        if (!(hasClient && isInvalidUsername && isLoggedIn)) {
            // all checks passed, user added to the list of clients, and the username is set
            return new Response(MessageType.ENTER_RESP, new Status("OK", 0));
        }

        return null;
    }
}
