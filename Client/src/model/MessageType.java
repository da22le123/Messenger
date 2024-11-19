package model;

/**
 * Represents the type of message that is being received
 */
public enum MessageType {
    READY,
    ENTER_RESP,
    PING,
    BROADCAST,
    BROADCAST_RESP,
    LEFT,
    BYE_RESP,
    UNKNOWN_MESSAGE_TYPE
}
