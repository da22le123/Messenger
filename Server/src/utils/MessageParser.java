package utils;

import model.messages.MessageType;

public class MessageParser {
    /**
     * Parses a message type from a string
     * @param message The message to be parsed
     * @return The message type
     */
    public static MessageType parseMessageType(String message) {
        return MessageType.valueOf(message);
    }

}
