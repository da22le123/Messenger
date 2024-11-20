package model.messages.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;
import model.messages.send.Sendable;

public class BroadcastRequest implements Sendable {
    @JsonProperty("message")
    private String message;

    public BroadcastRequest(@JsonProperty String message) {
        this.message = message;
    }

    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.BROADCAST_REQ + " " +  objectMapper.writeValueAsString(this);
    }
}
