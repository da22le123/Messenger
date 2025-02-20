package protocoltests.protocol.messages;

import model.messages.send.Status;

public record TttMoveResponse(Status status, String[] board) {
}
