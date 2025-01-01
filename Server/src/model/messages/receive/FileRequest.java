package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public record FileRequest(String recipient, String filename, String hash) implements Creator<FileRequest> {
    @Override
    public FileRequest create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, FileRequest.class);
    }

    public static FileRequest fromJson(String json) throws JsonProcessingException {
        return new FileRequest("", "", "").create(json);
    }
}
