package model.messages.send;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

public class RpsResult implements Sendable {
    @JsonProperty("status")
    private Status status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("game_result")
    private int gameResult;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("opponent_choice")
    private int opponentChoice;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("now_playing")
    private String[] nowPlaying; // usernames of the players that are playing

    public RpsResult(Status status) {
        this.status = status;
    }

    public RpsResult(Status status, int gameResult, int opponentChoice) {
        this.status = status;
        this.gameResult = gameResult;
        this.opponentChoice = opponentChoice;
    }
    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return MessageType.RPS_RESULT + " " + objectMapper.writeValueAsString(this);
    }

    @JsonIgnore
    public Status getStatus() {
        return status;
    }

    @JsonIgnore
    public int getGameResult() {
        return gameResult;
    }

    @JsonIgnore
    public int getOpponentChoice() {
        return opponentChoice;
    }

    @JsonIgnore
    public String[] getNowPlaying() {
        return nowPlaying;
    }

    @JsonIgnore
    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonIgnore
    public void setGameResult(int gameResult) {
        this.gameResult = gameResult;
    }

    @JsonIgnore
    public void setOpponentChoice(int opponentChoice) {
        this.opponentChoice = opponentChoice;
    }

    @JsonIgnore
    public void setNowPlaying(String[] nowPlaying) {
        this.nowPlaying = nowPlaying;
    }
}
