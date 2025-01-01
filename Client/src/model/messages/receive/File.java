package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record File(String sender, String filename) implements Creator<File> {
    @Override
    public File create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, File.class);
    }

    public static File fromJson(String json) throws JsonProcessingException {
        return new File("", "").create(json);
    }
}
