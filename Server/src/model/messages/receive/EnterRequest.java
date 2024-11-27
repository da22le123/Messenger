package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record EnterRequest(String username) implements Creator<EnterRequest> {
    @Override
    public EnterRequest create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, EnterRequest.class);
    }

    public static EnterRequest fromJson(String json) throws JsonProcessingException {
        return new EnterRequest("").create(json);
    }
}
