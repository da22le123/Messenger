package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.Status;

public record TttMoveResponse(Status status, String[] board) implements Creator<TttMoveResponse> {
    @Override
    public TttMoveResponse create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, TttMoveResponse.class);
    }

    public static TttMoveResponse fromJson(String json) throws JsonProcessingException {
        return new TttMoveResponse(new Status("", 0), new String[0]).create(json);
    }
}
