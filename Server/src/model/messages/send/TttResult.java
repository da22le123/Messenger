package model.messages.send;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.MessageType;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TttResult implements Sendable {
    @JsonProperty("status")
    private Status status;

    @JsonProperty("game_result")
    private int gameResult;

    @JsonProperty("now_playing")
    private String[] nowPlaying;

    @JsonProperty("board")
    private String[] board;

    // Private constructors prevent direct misuse
    private TttResult(Status status) {
        this.status = status;
    }

    private TttResult(Status status, int gameResult, String[] board) {
        this.status = status;
        this.gameResult = gameResult;
        this.board = board;
    }

    private TttResult(Status status, String[] nowPlaying) {
        this.status = status;
        this.nowPlaying = nowPlaying;
    }

    // Factory method for just status
    public static TttResult fromStatus(Status status) {
        return new TttResult(status);
    }

    // Factory method for status with game result and board
    public static TttResult fromStatusNGameResultNBoard(Status status, int gameResult, String[] board) {
        return new TttResult(status, gameResult, board);
    }

    // Factory method for status with now playing info
    public static TttResult fromStatusNNowPlaying(Status status, String[] nowPlaying) {
        return new TttResult(status, nowPlaying);
    }

    @Override
    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        // With the NON_DEFAULT inclusion, only non-default properties will be serialized.
        return MessageType.TTT_RESULT + " " + objectMapper.writeValueAsString(this);
    }
}
