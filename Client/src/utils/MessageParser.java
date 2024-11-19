package utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.MessageType;
import model.Message;
import model.Status;

/**
 * Parses messages received from the server
 */
public class MessageParser {
    /**
     * Parses a message type from a string
     * @param message The message to be parsed
     * @return The message type
     */
    public static MessageType parseMessageType(String message) {
        switch (message) {
            case "READY":
                return MessageType.READY;
            case "ENTER_RESP":
                return MessageType.ENTER_RESP;
            case "PING":
                return MessageType.PING;
            case "BROADCAST":
                return MessageType.BROADCAST;
            case "BROADCAST_RESP":
                return MessageType.BROADCAST_RESP;
            case "LEFT":
                return MessageType.LEFT;
            case "BYE_RESP":
                return MessageType.BYE_RESP;
            default:
                return MessageType.UNKNOWN_MESSAGE_TYPE;
        }
    }

    /**
     * Parses a status from a JSON string
     * @param statusJson The JSON string to be parsed
     * @return The status object
     * @throws JsonProcessingException
     */
    public static Status parseStatus(String statusJson) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(statusJson, Status.class);
    }

    /**
     * Parses a message from a JSON string
     * @param message The JSON string to be parsed
     * @return The message object
     * @throws JsonProcessingException
     */
    public static Message parseMessage(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Allow unquoted field names
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        return objectMapper.readValue(message, Message.class);
    }





}
