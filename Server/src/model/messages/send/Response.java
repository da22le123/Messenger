package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public class Response implements Sendable {
    private MessageType typeOfResponse;
    private Status status;

    public Response(MessageType typeOfResponse, Status status) {
        this.typeOfResponse = typeOfResponse;
        this.status = status;
    }

    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return typeOfResponse + " " + objectMapper.writeValueAsString(status);
    }
}
