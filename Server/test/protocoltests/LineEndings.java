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

class LineEndings {

    private final static Properties PROPS = new Properties();

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    private final static int MAX_DELTA_ALLOWED_MS = 300;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = LineEndings.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s.close();
    }

    @Test
    void tc21EnterFollowedByBROADCASTWithWindowsLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        String message = Utils.objectToMessage(new Enter("mynametc21")) + "\r\n" +
                Utils.objectToMessage(new BroadcastReq("a")) + "\r\n";
        out.print(message);
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        EnterResp enterResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", enterResp.status());

        serverResponse = receiveLineWithTimeout(in);
        BroadcastResp broadcastResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", broadcastResp.status());
    }

    @Test
    void tc22EnterFollowedByBROADCASTWithLinuxLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        String message = Utils.objectToMessage(new Enter("mynametc22")) + "\n" +
                Utils.objectToMessage(new BroadcastReq("a")) + "\n";
        out.print(message);
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        EnterResp enterResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", enterResp.status());

        serverResponse = receiveLineWithTimeout(in);
        BroadcastResp broadcastResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", broadcastResp.status());
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }

}