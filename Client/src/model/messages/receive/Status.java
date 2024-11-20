package model.messages.receive;

/**
 * Represents the status of a request
 */
public record Status(String status, int code) {
    public boolean isOk() {
        return this.status.equals("OK");
    }
}
