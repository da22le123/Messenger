package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record UserlistRequest() implements Creator<UserlistRequest> {
    @Override
    public UserlistRequest create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, UserlistRequest.class);
    }

    public static UserlistRequest fromJson(String json) throws JsonProcessingException {
        return new UserlistRequest().create(json);
    }
}
