package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.send.Rps;

public record RpsResponse(int choice) implements Creator<RpsResponse> {
    @Override
    public RpsResponse create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, RpsResponse.class);
    }

    public static RpsResponse fromJson(String json) throws JsonProcessingException {
        return new RpsResponse(0).create(json);
    }
}
