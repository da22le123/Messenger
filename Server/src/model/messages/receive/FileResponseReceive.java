package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.send.Status;

public record FileResponseReceive(String sender, Status status) implements Creator<FileResponseReceive> {
    @Override
    public FileResponseReceive create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, FileResponseReceive.class);
    }

    public static FileResponseReceive fromJson(String json) throws JsonProcessingException {
        return new FileResponseReceive("", new Status("", 0)).create(json);
    }
}
