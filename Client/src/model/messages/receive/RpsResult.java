package model.messages.receive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.Status;

public record RpsResult(@JsonProperty("status") Status status, @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("game_result") int gameResult, @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("opponent_choice") int opponentChoice, @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("now_playing") String[] nowPlaying) implements  Creator<RpsResult> {
    @Override
    public RpsResult create(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, RpsResult.class);
    }

    public static RpsResult fromJson(String json) throws JsonProcessingException {
        return new RpsResult(new Status("", 0), 0, 0, new String[0]).create(json);
    }
}
