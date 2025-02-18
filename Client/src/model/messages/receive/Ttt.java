package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record Ttt(String[] board) implements Creator<Ttt>{
    @Override
    public Ttt create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Ttt.class);
    }

    public static Ttt fromJson(String json) throws JsonProcessingException {
        return new Ttt(new String[0]).create(json);
    }
}
