package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.receive.DmRequest;
import model.messages.send.DirectMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.messages.DmResponse;
import protocoltests.protocol.messages.Enter;
import protocoltests.protocol.messages.UserList;
import protocoltests.protocol.messages.UserListRequest;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class UserListTests {

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
    public void tc62UserListIsReceivedWhenRequested() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //ready msg
        receiveLineWithTimeout(inUser2); //ready msg

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Enter("tc62user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect user 2
        outUser2.println(Utils.objectToMessage(new Enter("tc62user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK
        receiveLineWithTimeout(inUser1); //JOINED

        //send UserList req from user 1
        outUser1.println(Utils.objectToMessage(new UserListRequest()));
        outUser1.flush();

        String fromUser1 = receiveLineWithTimeout(inUser1);
        UserList userList = Utils.messageToObject(fromUser1);
        assertEquals(List.of("tc62user1", "tc62user2"), userList.users());
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }
}
