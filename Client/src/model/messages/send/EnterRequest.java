package model.messages.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;
import model.messages.send.Sendable;

public class EnterRequest implements Sendable {
    private String username;

    public EnterRequest(@JsonProperty String username) {
        this.username = username;
    }

    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.ENTER + objectMapper.writeValueAsString(this);
    }
}