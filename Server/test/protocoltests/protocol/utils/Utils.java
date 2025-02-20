package protocoltests.protocol.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.messages.receive.DmRequest;
import model.messages.send.DirectMessage;
import model.messages.send.Response;
import protocoltests.protocol.messages.*;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final static Map<Class<?>, String> objToNameMapping = new HashMap<>();
    static {
        objToNameMapping.put(UserListRequest.class, "USERLIST_REQ");
        objToNameMapping.put(UserList.class, "USERLIST");
        objToNameMapping.put(DmRequest.class, "DM_REQ");
        objToNameMapping.put(DmResponse.class, "DM_RESP");
        objToNameMapping.put(DirectMessage.class, "DM");
        objToNameMapping.put(Enter.class, "ENTER");
        objToNameMapping.put(EnterResp.class, "ENTER_RESP");
        objToNameMapping.put(BroadcastReq.class, "BROADCAST_REQ");
        objToNameMapping.put(BroadcastResp.class, "BROADCAST_RESP");
        objToNameMapping.put(Broadcast.class, "BROADCAST");
        objToNameMapping.put(Joined.class, "JOINED");
        objToNameMapping.put(ParseError.class, "PARSE_ERROR");
        objToNameMapping.put(Pong.class, "PONG");
        objToNameMapping.put(Ping.class, "PING");
        objToNameMapping.put(PongError.class, "PONG_ERROR");
        objToNameMapping.put(Ready.class, "READY");
        objToNameMapping.put(Rps.class, "RPS");
        objToNameMapping.put(RpsRequest.class, "RPS_REQ");
        objToNameMapping.put(RpsResponse.class, "RPS_RESP");
        objToNameMapping.put(TttRequest.class, "TTT_REQ");
        objToNameMapping.put(TttResponse.class, "TTT_RESP");
        objToNameMapping.put(TttResult.class, "TTT_RESULT");
        objToNameMapping.put(TttMove.class, "TTT_MOVE");
        objToNameMapping.put(TttMoveResponse.class, "TTT_MOVE_RESP");
        objToNameMapping.put(Ttt.class, "TTT");

    }

    public static String objectToMessage(Object object) throws JsonProcessingException {
        Class<?> clazz = object.getClass();
        String header = objToNameMapping.get(clazz);
        if (header == null) {
            throw new RuntimeException("Cannot convert this class to a message");
        }
        String body = mapper.writeValueAsString(object);
        return header + " " + body;
    }

    public static <T> T messageToObject(String message) throws JsonProcessingException {
        String[] parts = message.split(" ", 2);
        if (parts.length > 2 || parts.length == 0) {
            throw new RuntimeException("Invalid message");
        }
        String header = parts[0];
        String body = "{}";
        if (parts.length == 2) {
            body = parts[1];
        }
        Class<?> clazz = getClass(header);
        Object obj = mapper.readValue(body, clazz);
        return (T) clazz.cast(obj);
    }

    private static Class<?> getClass(String header) {
        return objToNameMapping.entrySet().stream()
                .filter(e -> e.getValue().equals(header))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find class belonging to header " + header));
    }
}
