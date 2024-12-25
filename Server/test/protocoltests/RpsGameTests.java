package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.messages.*;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class RpsGameTests {
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
    void tc63User2ReceivesRpsWhenUser1SendsRpsRequest() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc63user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc63user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // send RPS request
        int choice = 0; // rock
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc63user2", choice)));
        outUser1.flush();

        receiveLineWithTimeout(inUser2); //RPS
    }

    @Test
    void tc64User1SpecifiesIncorrectUsernameInRpsRequestReceivesError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc64user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc64user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // send RPS request
        int choice = 0; // rock
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc64user3", choice)));
        outUser1.flush();
        RpsResult rpsResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(3002, rpsResult.status().code());
    }

    @Test
    void tc65User1SendsRpsRequestWithIncorrectChoiceReceivesError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc65user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc65user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // send RPS request
        int choice = 3; // invalid choice
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc65user2", choice)));
        outUser1.flush();
        RpsResult rpsResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(3004, rpsResult.status().code());
    }

    @Test
    void tc66User1SendsRpsRequestSpecifyingThemselvesAsOpponentReceivesError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc66user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc66user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // send RPS request
        int choice = 0; // rock
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc66user1", choice)));
        outUser1.flush();
        RpsResult rpsResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(3003, rpsResult.status().code());
    }

    @Test
    void tc67UserSendsRpsRequestWhenGameIsRunningReceivesError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc67user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc67user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // send RPS request
        int choice = 0; // rock
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc67user2", choice)));
        outUser1.flush();
        receiveLineWithTimeout(inUser2); //RPS

        // send RPS request again
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc67user2", choice)));
        outUser1.flush();
        RpsResult rpsResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(3001, rpsResult.status().code());
        assertEquals("tc67user1", rpsResult.nowPlaying()[0]);
        assertEquals("tc67user2", rpsResult.nowPlaying()[1]);
    }

    @Test
    void tc68User2ThatReceivedTheRpsSpecifiesIncorrectChoiceBothUsersReceiveError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc68user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc68user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // send RPS request
        int choice = 0; // rock
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc68user2", choice)));
        outUser1.flush();

        receiveLineWithTimeout(inUser2); //RPS
        int incorrectChoice = 3; // invalid choice
        outUser2.println(Utils.objectToMessage(new RpsResponse(incorrectChoice)));
        outUser2.flush();

        RpsResult rpsResult1 = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        RpsResult rpsResult2 = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        assertEquals(3005, rpsResult1.status().code());
        assertEquals(3005, rpsResult2.status().code());
    }

    @Test
    void tc69ClientSendsRequestWhileNotLoggedInReceivesError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // send RPS request
        int choice = 0; // rock
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc69user2", choice)));
        outUser1.flush();
        RpsResult rpsResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(3000, rpsResult.status().code());
    }

    @Test
    void tc70User1SendsValidRpsRequestUser2RespondsWithValidChoiceBothUsersReceiveResult() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc70user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc70user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // send RPS request
        int choice = 0; // rock
        outUser1.println(Utils.objectToMessage(new RpsRequest("tc70user2", choice)));
        outUser1.flush();

        receiveLineWithTimeout(inUser2); //RPS
        int responseChoice = 1; // paper
        outUser2.println(Utils.objectToMessage(new RpsResponse(responseChoice)));
        outUser2.flush();

        RpsResult rpsResult1 = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        RpsResult rpsResult2 = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        assertEquals(0, rpsResult1.status().code()); //ok
        assertEquals(0, rpsResult2.status().code()); //ok
        assertEquals(1, rpsResult1.opponentChoice());
        assertEquals(0, rpsResult2.opponentChoice());
        assertEquals(1, rpsResult1.gameResult());
        assertEquals(1, rpsResult2.gameResult());
    }



    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }
}
