package model.messages.send;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Sendable {
    String toJson() throws JsonProcessingException;
}
