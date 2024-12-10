package model.messages.receive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the status of a request
 */
public record Status(String status, int code) implements Creator<Status>{
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
}
