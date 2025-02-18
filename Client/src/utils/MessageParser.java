package utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;
import model.messages.receive.ReceivedBroadcastMessage;
import model.messages.Status;
import model.messages.receive.UserlistMessage;

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
        return MessageType.valueOf(message);
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
    public static ReceivedBroadcastMessage parseMessage(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        // Allow unquoted field names
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        return objectMapper.readValue(message, ReceivedBroadcastMessage.class);
    }

    public static UserlistMessage parseUserlist(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(message, UserlistMessage.class);
    }






}
