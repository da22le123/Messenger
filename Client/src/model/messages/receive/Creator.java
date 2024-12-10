package model.messages.receive;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface Creator<T> {
    T create(String json) throws JsonProcessingException;
}