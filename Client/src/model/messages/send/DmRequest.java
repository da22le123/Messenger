package model.messages.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public record DmRequest(@JsonProperty("recipient") String recipient, @JsonProperty("message") String message) implements Sendable{

    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.DM_REQ + " " + objectMapper.writeValueAsString(this);
    }
}
