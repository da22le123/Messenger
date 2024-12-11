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
    HANGUP,
    USERLIST_REQ,
    USERLIST,
    RPS,
    RPS_REQ,
    RPS_RESP,
    RPS_RESULT,
    DM_REQ,
    DM_RESP,
    DM,
    PARSE_ERROR,
    UNKNOWN_COMMAND
}
