package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

import java.util.ArrayList;
import java.util.List;

public record UserlistMessage(List<String> users) implements Sendable {
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.USERLIST + " " + objectMapper.writeValueAsString(this);
    }
}
