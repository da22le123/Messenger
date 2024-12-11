package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record RpsRequest(String opponent, int choice) implements Creator<RpsRequest> {
    @Override
    public RpsRequest create(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(message, RpsRequest.class);
    }

    public static RpsRequest fromJson(String json) throws JsonProcessingException {
        return new RpsRequest("", 0).create(json);
    }

}
