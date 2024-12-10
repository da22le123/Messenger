package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.MessageType;
import model.messages.messagefactories.StatusFactory;
import model.messages.receive.DmRequest;
import model.messages.send.DirectMessage;
import model.messages.send.Response;
import model.messages.send.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.messages.BroadcastReq;
import protocoltests.protocol.messages.BroadcastResp;
import protocoltests.protocol.messages.DmResponse;
import protocoltests.protocol.messages.Enter;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

public class DirectMessagesTests {

    private final static Properties PROPS = new Properties();

    private Socket socketUser1, socketUser2, socketUser3;
    private BufferedReader inUser1, inUser2, inUser3;
    private PrintWriter outUser1, outUser2, outUser3;

    private final static int MAX_DELTA_ALLOWED_MS = 300;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = DirectMessagesTests.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        socketUser1 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        socketUser2 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);

        socketUser3 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inUser3 = new BufferedReader(new InputStreamReader(socketUser3.getInputStream()));
        outUser3 = new PrintWriter(socketUser3.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        socketUser2.close();
        socketUser3.close();
    }

    @Test
    public void tc58DirectMessageIsReceivedByTheRecipient() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc58user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 1
        outUser2.println(Utils.objectToMessage(new Enter("tc58user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //send DM from user 1
        outUser1.println(Utils.objectToMessage(new DmRequest("tc58user2", "Hello user2")));
        outUser1.flush();

        String fromUser1 = receiveLineWithTimeout(inUser1);
        DmResponse dmResponse = Utils.messageToObject(fromUser1);
        assertEquals("OK", dmResponse.status());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        DirectMessage directMessage = Utils.messageToObject(fromUser2);
        assertEquals("Hello user2", directMessage.message());
    }

    @Test
    public void tc59DirectMessageWithNonExistentRecipientReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc59user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 1
        outUser2.println(Utils.objectToMessage(new Enter("tc59user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //send DM from user 1
        outUser1.println(Utils.objectToMessage(new DmRequest("nonexistentUser", "Hello user2")));
        outUser1.flush();

        String fromUser1 = receiveLineWithTimeout(inUser1);
        DmResponse dmResponse = Utils.messageToObject(fromUser1);
        assertEquals("ERROR", dmResponse.status());
    }

    @Test
    public void tc60DirectMessageIsNotReceivedByOtherUsers() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc60user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 1
        outUser2.println(Utils.objectToMessage(new Enter("tc60user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        outUser3.println(Utils.objectToMessage(new Enter("tc60user3")));
        outUser3.flush();
        receiveLineWithTimeout(inUser3); //OK
        receiveLineWithTimeout(inUser1); //JOINED
        receiveLineWithTimeout(inUser2); //JOINED

        //send DM from user 1
        outUser1.println(Utils.objectToMessage(new DmRequest("tc60user2", "Hello user2")));
        outUser1.flush();

        String fromUser1 = receiveLineWithTimeout(inUser1);
        DmResponse dmResponse = Utils.messageToObject(fromUser1);
        assertEquals("OK", dmResponse.status());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        DirectMessage directMessage = Utils.messageToObject(fromUser2);
        assertEquals("Hello user2", directMessage.message());

        // assert user 3 did not receive the DM
        String lastReceivedMessage = receiveLineWithTimeout(inUser3);
        assertEquals("ENTER_RESP {\"status\":\"OK\",\"code\":0}", lastReceivedMessage);
    }


    @Test
    public void tc61DirectMessageWhenNotLoggedInReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        outUser1.println(Utils.objectToMessage(new DmRequest("tc60user2", "Hello user2")));
        outUser1.flush();

        String fromUser1 = receiveLineWithTimeout(inUser1);
        DmResponse dmResponse = Utils.messageToObject(fromUser1);
        assertEquals("ERROR", dmResponse.status());
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }
}
