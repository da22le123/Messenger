package protocoltests.protocol.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.messages.send.Status;

public record RpsResult(Status status,
                        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("game_result") int gameResult,
                        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("opponent_choice") int opponentChoice,
                        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("now_playing") String[] nowPlaying) {}
