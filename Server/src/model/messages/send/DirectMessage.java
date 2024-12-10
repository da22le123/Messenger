package model.messages.send;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public record DirectMessage(String sender, String message) implements Sendable {
    @JsonIgnore
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.DM + " " + objectMapper.writeValueAsString(this);
    }
}
