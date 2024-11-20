import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import utils.MessageParser;
import model.messages.receive.Status;

import static org.junit.jupiter.api.Assertions.*;

public class TestMessageParser {
    @Test
    public void GivenStatusOk_WhenParseMessage_ReturnsStatusObjectWithOkStatusAndCodeZero() throws JsonProcessingException {
        String statusToParse = "{\"status\":\"OK\"}";

        Status status = MessageParser.parseStatus(statusToParse);
        assertEquals("Status[status=OK, code=0]", status.toString());
        assertEquals("OK", status.status());
        assertEquals(0, status.code());
        assertTrue(status.isOk());
    }

}
