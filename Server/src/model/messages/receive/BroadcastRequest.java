package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record BroadcastRequest(String message) implements Creator<BroadcastRequest>{
    @Override
    public BroadcastRequest create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, BroadcastRequest.class);
    }

    public static BroadcastRequest fromJson(String json) throws JsonProcessingException {
        return new BroadcastRequest("").create(json);
    }
}
