package model.messages.send;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Status(String status, int code) {
    @JsonIgnore
    public boolean isOk() {
        return this.status.equals("OK");
    }
}
