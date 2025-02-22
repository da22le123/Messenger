package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import jdk.jshell.execution.Util;
import model.messages.send.Status;
import model.messages.send.TttRequestSend;
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

public class TttGameTests {
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
        outUser1.println(Utils.objectToMessage(new TttRequest("tc71user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        TttRequest tttRequest = Utils.messageToObject(receiveLineWithTimeout(inUser2));
        assertEquals("tc71user1", tttRequest.opponent());
        for (int i = 0; i < 9; i++) {
            String[] expectedResult = new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."};
            assertEquals(expectedResult[i], tttRequest.board()[i]);
        }
    }

    @Test
    public void tc72User1ReceivesTheTttResultWhenUser2RejectsTttRequest() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc72user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc72user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // User 1 sends TTT request
        outUser1.println(Utils.objectToMessage(new TttRequest("tc72user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 2 receives TTT request
        TttRequest tttRequest = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        // User 2 rejects TTT request
        outUser2.println(Utils.objectToMessage(new TttResponse(new Status("ERROR", 2005))));
        outUser2.flush();

        // User 1 receives TTT result
        TttResult tttResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(2005, tttResult.status().code());
    }

    @Test
    public void tc73User1ReceivesTttResultWhenHeSendsTttResponseWithoutBeingChallengedToGame() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc73user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc73user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // User 1 sends TTT response
        outUser1.println(Utils.objectToMessage(new TttResponse(new Status("OK", 0))));
        outUser1.flush();

        // User 1 receives TTT result
        TttResult tttResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(2007, tttResult.status().code());
    }

    @Test
    public void tc74User1ReceivesTttMoveResponseCode2004WhenSelectsAlreadyTakenPosition() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc74user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc74user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // User 1 sends TTT request
        outUser1.println(Utils.objectToMessage(new TttRequest("tc74user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 2 receives TTT request
        TttRequest tttRequest = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        // User 2 accepts TTT request
        outUser2.println(Utils.objectToMessage(new TttResponse(new Status("OK", 0))));
        outUser2.flush();

        // User 2 sends TTT move on already taken position
        outUser2.println(Utils.objectToMessage(new TttMove(new String[]{"O", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser2.flush();

        // User 2 receives TTT move response code 2004
        TttMoveResponse tttMoveResponse = Utils.messageToObject(receiveLineWithTimeout(inUser2));
        assertEquals(2004, tttMoveResponse.status().code());
    }

    @Test
    public void tc75BothUsersAreShownTheUpdatedBoardWhenOneUserMakesAMove() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc75user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc75user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // User 1 sends TTT request
        outUser1.println(Utils.objectToMessage(new TttRequest("tc75user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 2 receives TTT request
        TttRequest tttRequest = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        // User 2 accepts TTT request
        outUser2.println(Utils.objectToMessage(new TttResponse(new Status("OK", 0))));
        outUser2.flush();

        // User 2 sends TTT move
        outUser2.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", ".", ".", ".", ".", ".", "."})));
        outUser2.flush();

        // BOTH USERS RECEIVE UPDATED BOARD

        // User 1 receives TTT
        Ttt ttt = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        for (int i = 0; i < 9; i++) {
            String[] expectedResult = new String[]{"X", "O", ".", ".", ".", ".", ".", ".", "."};
            assertEquals(expectedResult[i], ttt.board()[i]);
        }

        // User 2 receives TTT Move response
        TttMoveResponse tttMoveResponse = Utils.messageToObject(receiveLineWithTimeout(inUser2));
        for (int i = 0; i < 9; i++) {
            String[] expectedResult = new String[]{"X", "O", ".", ".", ".", ".", ".", ".", "."};
            assertEquals(expectedResult[i], tttMoveResponse.board()[i]);
        }
    }

    @Test
    public void tc76User3ReceivesTttResultCode2001AndCurrentPlayersWhenSendsRequestWhileTttGameIsRunning() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg
        receiveLineWithTimeout(inUser3); //ready msg


        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc76user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc76user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // Connect user 3
        outUser3.println(Utils.objectToMessage(new Enter("tc76user3")));
        outUser3.flush();
        receiveLineWithTimeout(inUser3); //OK
        receiveLineWithTimeout(inUser1); //JOINED
        receiveLineWithTimeout(inUser2); //JOINED

        // User 1 sends TTT request
        outUser1.println(Utils.objectToMessage(new TttRequest("tc76user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 2 receives TTT request
        TttRequest tttRequest = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        // User 2 accepts TTT request
        outUser2.println(Utils.objectToMessage(new TttResponse(new Status("OK", 0))));
        outUser2.flush();

        // User 2 sends TTT move
        outUser2.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", ".", ".", ".", ".", ".", "."})));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //TTT MOVE RESP

        // User 3 sends TTT request while TTT game is running
        outUser3.println(Utils.objectToMessage(new TttRequest("tc76user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser3.flush();

        // User 3 receives TTT result code 2001 and current players
        TttResult tttResult = Utils.messageToObject(receiveLineWithTimeout(inUser3));
        assertEquals(2001, tttResult.status().code());
        assertEquals("tc76user1", tttResult.nowPlaying()[0]);
        assertEquals("tc76user2", tttResult.nowPlaying()[1]);
    }

    @Test
    public void tc77BothUsersReceiveResultWhenTttGameEnds() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc77user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc77user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // User 1 sends TTT request
        outUser1.println(Utils.objectToMessage(new TttRequest("tc77user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 2 receives TTT request
        TttRequest tttRequest = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        // User 2 accepts TTT request
        outUser2.println(Utils.objectToMessage(new TttResponse(new Status("OK", 0))));
        outUser2.flush();

        // User 2 sends TTT move
        outUser2.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", ".", ".", ".", ".", ".", "."})));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //TTT MOVE RESP
        receiveLineWithTimeout(inUser1); //TTT

        // User 1 sends TTT move
        outUser1.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", "X", ".", ".", ".", ".", "."})));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //TTT MOVE RESP
        receiveLineWithTimeout(inUser2); //TTT

        // User 2 sends TTT move
        outUser2.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", "X", "O", ".", ".", ".", "."})));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //TTT MOVE RESP
        receiveLineWithTimeout(inUser1); //TTT

        // User 1 sends TTT move
        outUser1.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", "X", "O", ".", "X", ".", "."})));
        outUser1.flush();


        TttResult tttResultUser1 = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        TttResult tttResultUser2 = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        assertEquals(0, tttResultUser1.status().code());
        assertEquals(0, tttResultUser2.status().code());

        assertEquals(0, tttResultUser1.gameResult());
        assertEquals(0, tttResultUser2.gameResult());
        System.out.println(Utils.objectToMessage(tttResultUser1));
    }

    @Test
    public void tc78User1ReceivesTheTttResultCode2002WhenSpecifiedNonExistentUserAsOpponent() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc78user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // User 1 sends TTT request to non-existent user
        outUser1.println(Utils.objectToMessage(new TttRequest("nonExistentUser",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 1 receives TTT result code 2002
        TttResult tttResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(2002, tttResult.status().code());
    }

    @Test
    public void tc79User1ReceivesTheTttResultCode2003WhenSpecifiedThemselvesAsOpponent() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc79user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // User 1 sends TTT request to himself
        outUser1.println(Utils.objectToMessage(new TttRequest("tc79user1",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 1 receives TTT result code 2003
        TttResult tttResult = Utils.messageToObject(receiveLineWithTimeout(inUser1));
        assertEquals(2003, tttResult.status().code());
    }

    @Test
    public void tc80User2ReceivesTheTttResultCode2006WhenSendingTttMoveOutOfTurn() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc80user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc80user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        // User 1 sends TTT request
        outUser1.println(Utils.objectToMessage(new TttRequest("tc80user2",
                new String[]{"X", ".", ".", ".", ".", ".", ".", ".", "."})));
        outUser1.flush();

        // User 2 receives TTT request
        TttRequest tttRequest = Utils.messageToObject(receiveLineWithTimeout(inUser2));

        // User 2 accepts TTT request
        outUser2.println(Utils.objectToMessage(new TttResponse(new Status("OK", 0))));
        outUser2.flush();

        // User 2 sends TTT move
        outUser2.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", ".", ".", ".", ".", ".", "."})));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //TTT MOVE RESP
        receiveLineWithTimeout(inUser1); //TTT

        // User 2 sends TTT move out of turn
        outUser2.println(Utils.objectToMessage(new TttMove(new String[]{"X", "O", ".", "X", ".", ".", ".", ".", "."})));
        outUser2.flush();

        TttMoveResponse tttMoveResponse = Utils.messageToObject(receiveLineWithTimeout(inUser2));
        assertEquals(2006, tttMoveResponse.status().code());

    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }
}
