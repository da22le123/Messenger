package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import protocoltests.protocol.messages.*;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class MultipleUserTests {

    private final static Properties PROPS = new Properties();

    private Socket socketUser1, socketUser2;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2;

    private final static int MAX_DELTA_ALLOWED_MS = 300;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = MultipleUserTests.class.getResourceAsStream("testconfig.properties");
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
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        socketUser2.close();
    }

    @Test
    void tc31JoinedIsReceivedByOtherUserWhenUserConnects() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Enter("user1tc31")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Enter("user2tc31")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK

        //JOINED is received by user1 when user2 connects
        /* This test is expected to fail with the given NodeJS server because the JOINED is not implemented.
           Make sure the test works when implementing your own server in Java */
        String resIdent = receiveLineWithTimeout(inUser1);
        Joined joined = Utils.messageToObject(resIdent);

        assertEquals(new Joined("user2tc31"), joined);
    }

    @Test
    void tc32BroadcastMessageIsReceivedByOtherConnectedClients() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Enter("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Enter("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        /* This test is expected to fail with the given NodeJS server because the JOINED is not implemented.
           Make sure the test works when implementing your own server in Java */
        receiveLineWithTimeout(inUser1); //JOINED

        //send BROADCAST from user 1
        outUser1.println(Utils.objectToMessage(new BroadcastReq("messagefromuser1")));

        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1);
        BroadcastResp broadcastResp1 = Utils.messageToObject(fromUser1);

        assertEquals("OK", broadcastResp1.status());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        Broadcast broadcast2 = Utils.messageToObject(fromUser2);

        assertEquals(new Broadcast("user1", "messagefromuser1"), broadcast2);

        //send BROADCAST from user 2
        outUser2.println(Utils.objectToMessage(new BroadcastReq("messagefromuser2")));
        outUser2.flush();
        fromUser2 = receiveLineWithTimeout(inUser2);
        BroadcastResp broadcastResp2 = Utils.messageToObject(fromUser2);
        assertEquals("OK", broadcastResp2.status());

        fromUser1 = receiveLineWithTimeout(inUser1);
        Broadcast broadcast1 = Utils.messageToObject(fromUser1);

        assertEquals(new Broadcast("user2", "messagefromuser2"), broadcast1);
    }

    @Test
    void tc33EnterMessageWithAlreadyConnectedUsernameReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready message
        receiveLineWithTimeout(inUser2); //ready message

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect using same username
        outUser2.println(Utils.objectToMessage(new Enter("user1")));
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2);
        EnterResp enterResp = Utils.messageToObject(resUser2);
        assertEquals(new EnterResp("ERROR", 5000), enterResp);
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }

}