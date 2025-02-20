package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.send.TttRequestSend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.messages.Enter;
import protocoltests.protocol.messages.TttRequest;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class TttGameTests {
    private final static Properties PROPS = new Properties();

    private Socket socketUser1, socketUser2, socketUser3;
    private BufferedReader inUser1, inUser2, inUser3;
    private PrintWriter outUser1, outUser2, outUser3;

    private final static int MAX_DELTA_ALLOWED_MS = 500;

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

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }

    @Test
    public void tc71User2ReceivesTheTttRequestWhenUser1SendsIt() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc71user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc71user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // User 1 sends TTT request
        outUser1.println(Utils.objectToMessage(new TttRequest("tc71user2", 0)));
        outUser1.flush();

        String response = receiveLineWithTimeout(inUser2);
        String payload = response.split(" ", 2)[1];
        TttRequestSend tttRequest = TttRequestSend.fromJson(payload);
        assertEquals("tc71user1", tttRequest.opponent());
        assertEquals(0, tttRequest.move());
    }
}
