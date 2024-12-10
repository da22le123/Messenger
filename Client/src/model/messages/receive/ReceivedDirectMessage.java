package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record ReceivedDirectMessage(String sender, String message) implements Creator<ReceivedDirectMessage>{
    @Override
    public ReceivedDirectMessage create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(json, ReceivedDirectMessage.class);
    }

    public static ReceivedDirectMessage fromJson(String json) throws JsonProcessingException {
        return new ReceivedDirectMessage("", "").create(json);
    }
}
