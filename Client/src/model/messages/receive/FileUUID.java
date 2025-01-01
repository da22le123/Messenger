package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record FileUUID(String uuid) implements Creator<FileUUID> {
    @Override
    public FileUUID create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, FileUUID.class);
    }

    public static FileUUID fromJson(String json) throws JsonProcessingException {
        return new FileUUID("").create(json);
    }
}
