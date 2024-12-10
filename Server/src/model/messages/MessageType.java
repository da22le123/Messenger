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
    USERLIST_REQ,
    USERLIST,
    DM_REQ,
    DM_RESP,
    DM,
    JOINED,
    LEFT,
    BYE,
    BYE_RESP,
    HANGUP,
    PARSE_ERROR,
    PONG_ERROR,
    UNKNOWN_COMMAND
}
