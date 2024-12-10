package model.messages.receive;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record DmRequest(@JsonProperty String recipient, @JsonProperty String message) implements Creator<DmRequest>{
    @Override
    public DmRequest create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, DmRequest.class);
    }

    public static DmRequest fromJson(String json) throws JsonProcessingException {
        return new DmRequest("", "").create(json);
    }
}
