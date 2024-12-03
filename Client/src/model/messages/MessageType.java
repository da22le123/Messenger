package model.messages;

/**
 * Represents the type of message that is being received
 */
public enum MessageType {
    READY,
    ENTER,
    ENTER_RESP,
    PING,
    PONG,
    BROADCAST,
    BROADCAST_REQ,
    BROADCAST_RESP,
    JOINED,
    LEFT,
    BYE,
    BYE_RESP,
    PARSE_ERROR,
    UNKNOWN_COMMAND
}
