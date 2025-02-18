package model.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.receive.Creator;
import model.messages.send.Sendable;

/**
 * Represents the status of a request
 */
public record Status(String status, int code) implements Creator<Status>, Sendable {
    @Override
    public Status create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Status.class);
    }

    public static Status fromJson(String json) throws JsonProcessingException {
        return new Status("", 0).create(json);
    }

    @JsonIgnore
    public boolean isOk() {
        return this.status.equals("OK");
    }

    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
