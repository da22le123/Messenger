package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record TttRequestReceive(String opponent, String[] board) implements Creator<TttRequestReceive> {
    @Override
    public TttRequestReceive create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, TttRequestReceive.class);
    }

    public static TttRequestReceive fromJson(String json) throws JsonProcessingException {
        return new TttRequestReceive("", new String[0]).create(json);
    }
}
