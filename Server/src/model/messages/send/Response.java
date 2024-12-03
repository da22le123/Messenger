package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public record Response(MessageType typeOfResponse, Status status) implements Sendable {
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("String value of status: " + objectMapper.writeValueAsString(status));
        return typeOfResponse + " " + objectMapper.writeValueAsString(status);

    }
}
