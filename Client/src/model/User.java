package model;

/**
 * This record is used solely for parsing the username from the JSON message that informs other user has left the chat.
 * @param username
 */
public record User(String username) {
}
