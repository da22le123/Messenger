package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;
import model.messages.receive.Creator;

public record RpsRequest(String opponent, int choice) implements Sendable {
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.RPS_REQ + " " + objectMapper.writeValueAsString(this);
    }
}
