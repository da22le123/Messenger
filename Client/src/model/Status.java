package model;

public record Status(String status, int code) {
    public boolean isOk() {
        return this.status.equals("OK");
    }
}
