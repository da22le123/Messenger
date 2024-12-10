package model.messages.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public class DirectMessageRequest implements Sendable{
    @JsonProperty("recipient")
    private final String recipient;
    @JsonProperty("message")
    private final String message;

    public DirectMessageRequest(@JsonProperty("recipient") String recipient,@JsonProperty("message") String message) {
        this.recipient = recipient;
        this.message = message;
    }

    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.DM_REQ + " " + objectMapper.writeValueAsString(this);
    }
}
