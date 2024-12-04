package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public record HangupMessage(int reason) implements Sendable{
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.HANGUP + " " + objectMapper.writeValueAsString(this);
    }
}
