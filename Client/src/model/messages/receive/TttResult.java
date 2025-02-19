package model.messages.receive;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.Status;

public record TttResult(Status status, @JsonProperty("game_result") int gameResult,@JsonProperty("board")  String[] board,@JsonProperty("now_playing")  String[] nowPlaying) implements Creator<TttResult>{
    @Override
    public TttResult create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, TttResult.class);
    }

    public static TttResult fromJson(String json) throws JsonProcessingException {
        return new TttResult(new Status("", 0), 0, new String[0], new String[0]).create(json);
    }
}
