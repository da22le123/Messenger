package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.send.Status;

public record TttResponse(Status status) implements Creator<TttResponse> {
    @Override
    public TttResponse create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, TttResponse.class);
    }

    public static TttResponse fromJson(String json) throws JsonProcessingException {
        return new TttResponse(new Status("", 0)).create(json);
    }
}
