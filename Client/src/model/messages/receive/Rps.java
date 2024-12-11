package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record Rps(String opponent) implements Creator<Rps> {
    @Override
    public Rps create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Rps.class);
    }

    public static Rps fromJson(String json) throws JsonProcessingException {
        return new Rps("").create(json);
    }
}
