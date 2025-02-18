package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public record TttMoveResponse(Status status, String[] board) implements Sendable{
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.TTT_MOVE_RESP + " " + objectMapper.writeValueAsString(this);
    }
}
