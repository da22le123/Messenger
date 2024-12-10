package model;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.send.Sendable;

import java.io.PrintWriter;

public class MessageSender {
    private final PrintWriter out;
    public MessageSender(PrintWriter out) {
        this.out = out;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendMessage(Sendable message) throws JsonProcessingException {
        out.println(message.toJson());
    }
}
