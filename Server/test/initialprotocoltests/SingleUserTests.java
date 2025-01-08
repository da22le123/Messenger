package initialprotocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import protocoltests.protocol.messages.*;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class SingleUserTests {

    private final static Properties PROPS = new Properties();
    private static int pingTimeMs;
    private static int pingTimeMsDeltaAllowed;
    private final static int MAX_DELTA_ALLOWED_MS = 300;

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = SingleUserTests.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        in.close();

        pingTimeMs = Integer.parseInt(PROPS.getProperty("ping_time_ms", "10000"));
        pingTimeMsDeltaAllowed = Integer.parseInt(PROPS.getProperty("ping_time_ms_delta_allowed", "100"));
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
    void tc51InitialConnectionToServerReturnsReadyMessage() throws JsonProcessingException {
        String firstLine = receiveLineWithTimeout(in);
        Ready ready = Utils.messageToObject(firstLine);
        assertEquals(new Ready("1.6.0"), ready);
    }

    @Test
    void tc52ValidIdentMessageReturnsOkMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        out.println(Utils.objectToMessage(new Enter("myname")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        EnterResp enterResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", enterResp.status());
    }

    @Test
    void tc53InvalidJsonMessageReturnsParseError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        out.println("ENTER {\"}");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ParseError parseError = Utils.messageToObject(serverResponse);
        assertNotNull(parseError);
    }

    @Test
    void tc54EmptyJsonMessageReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        out.println("ENTER ");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        EnterResp enterResp = Utils.messageToObject(serverResponse);
        assertEquals(new EnterResp("ERROR", 5001), enterResp);
    }

    @Test
    void tc55PongWithoutPingReturnsErrorMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        out.println(Utils.objectToMessage(new Pong()));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        PongError pongError = Utils.messageToObject(serverResponse);
        assertEquals(new PongError(8000), pongError);
    }

    @Test
    void tc56EnterTwiceReturnsErrorMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        out.println(Utils.objectToMessage(new Enter("first")));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        EnterResp enterResp = Utils.messageToObject(serverResponse);
        assertEquals("OK", enterResp.status());

        out.println(Utils.objectToMessage(new Enter("second")));
        out.flush();
        serverResponse = receiveLineWithTimeout(in);
        enterResp = Utils.messageToObject(serverResponse);
        assertEquals(new EnterResp("ERROR", 5002), enterResp);
    }

    @Test
    void tc57PingIsReceivedAtExpectedTime(TestReporter testReporter) throws JsonProcessingException {
        receiveLineWithTimeout(in); //ready message
        out.println(Utils.objectToMessage(new Enter("myname")));
        out.flush();
        receiveLineWithTimeout(in); //server response

        //Make sure the test does not hang when no response is received by using assertTimeoutPreemptively
        assertTimeoutPreemptively(ofMillis(pingTimeMs + pingTimeMsDeltaAllowed), () -> {
            Instant start = Instant.now();
            String pingString = in.readLine();
            Instant finish = Instant.now();

            // Make sure the correct response is received
            Ping ping = Utils.messageToObject(pingString);

            assertNotNull(ping);

            // Also make sure the response is not received too early
            long timeElapsed = Duration.between(start, finish).toMillis();
            testReporter.publishEntry("timeElapsed", String.valueOf(timeElapsed));
            assertTrue(timeElapsed > pingTimeMs - pingTimeMsDeltaAllowed);
        });
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }

}