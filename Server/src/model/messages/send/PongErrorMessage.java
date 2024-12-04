package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public record PongErrorMessage(int code) implements Sendable {
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.PONG_ERROR + " " + objectMapper.writeValueAsString(this);
    }
}
