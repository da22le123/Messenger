package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;
import model.messages.receive.Status;

public record FileResponseSend(String sender, Status status) implements Sendable{
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.FILE_RESP + " " + objectMapper.writeValueAsString(this);
    }
}
