package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record TttMove(String[] board) implements Creator<TttMove> {
    @Override
    public TttMove create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, TttMove.class);
    }

    public static TttMove fromJson(String json) throws JsonProcessingException {
        return new TttMove(new String[0]).create(json);
    }
}
