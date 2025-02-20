package protocoltests.protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.messages.send.Status;

public record TttResult(@JsonProperty("status") Status status, @JsonProperty("game_result") int gameResult, @JsonProperty("now_playing") String[] nowPlaying, @JsonProperty("board") String[] board) {
}
