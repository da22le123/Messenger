# Protocol description

This client-server protocol describes the following scenarios:

- Setting up a connection between client and server.
- Broadcasting a message to all connected clients.
- Periodically sending heartbeat to connected clients.
- Disconnection from the server.
- Handling invalid messages.

In the description below, `C -> S` represents a message from the client `C` is send to server `S`. When applicable, `C`
is extended with a number to indicate a specific client, e.g., `C1`, `C2`, etc. The keyword `others` is used to indicate
all other clients except for the client who made the request. Messages can contain a JSON body. Text shown between `<`
and `>` are placeholders.

The protocol follows the formal JSON specification, RFC 8259, available on https://www.rfc-editor.org/rfc/rfc8259.html

# 1. Establishing a connection

The client first sets up a socket connection to which the server responds with a welcome message. The client supplies a
username on which the server responds with an OK if the username is accepted or an ERROR with a number in case of an
error.
_Note:_ A username may only consist of characters, numbers, and underscores ('_') and has a length between 3 and 14
characters.

## 1.1 Happy flow

Client sets up the connection with server.

```
S -> C: READY {"version": "<server version number>"}
```

- `<server version number>`: the semantic version number of the server.

After a while when the client logs the user in:

```
C -> S: ENTER {"username":"<username>"}
S -> C: ENTER_RESP {"status":"OK"}
```

- `<username>`: the username of the user that needs to be logged in.
  To other clients (Only applicable when working on Level 2):

```
S -> others: JOINED {"username":"<username>"}
```

## 1.2 Unhappy flow

```
S -> C: ENTER_RESP {"status":"ERROR", "code":<error code>}
```      

Possible `<error code>`:

| Error code | Description                              |
|------------|------------------------------------------|
| 5000       | User with this name already exists       |
| 5001       | Username has an invalid format or length |      
| 5002       | Already logged in                        |

# 2. Broadcast message

Sends a message from a client to all other clients. The sending client does not receive the message itself but gets a
confirmation that the message has been sent.

## 2.1 Happy flow

```
C -> S: BROADCAST_REQ {"message":"<message>"}
S -> C: BROADCAST_RESP {"status":"OK"}
```

- `<message>`: the message that must be sent.

Other clients receive the message as follows:

```
S -> others: BROADCAST {"username":"<username>","message":"<message>"}   
```   

- `<username>`: the username of the user that is sending the message.

## 2.2 Unhappy flow

```
S -> C: BROADCAST_RESP {"status": "ERROR", "code": <error code>}
```

Possible `<error code>`:

| Error code | Description           |
|------------|-----------------------|
| 6000       | User is not logged in |

# 3. Heartbeat message

Sends a ping message to the client to check whether the client is still active. The receiving client should respond with
a pong message to confirm it is still active. If after 3 seconds no pong message has been received by the server, the
connection to the client is closed. Before closing, the client is notified with a HANGUP message, with reason code 7000.

The server sends a ping message to a client every 10 seconds. The first ping message is send to the client 10 seconds
after the client is logged in.

When the server receives a PONG message while it is not expecting one, a PONG_ERROR message will be returned.

## 3.1 Happy flow

```
S -> C: PING
C -> S: PONG
```     

## 3.2 Unhappy flow

```
S -> C: HANGUP {"reason": <reason code>}
[Server disconnects the client]
```      

Possible `<reason code>`:

| Reason code | Description      |
|-------------|------------------|
| 7000        | No pong received |    

```
S -> C: PONG_ERROR {"code": <error code>}
```

Possible `<error code>`:

| Error code | Description       |
|------------|-------------------|
| 8000       | Pong without ping |    

# 4. Termination of the connection

When the connection needs to be terminated, the client sends a bye message. This will be answered (with a BYE_RESP
message) after which the server will close the socket connection.

## 4.1 Happy flow

```
C -> S: BYE
S -> C: BYE_RESP {"status":"OK"}
[Server closes the socket connection]
```

Other, still connected clients, clients receive:

```
S -> others: LEFT {"username":"<username>"}
```

## 4.2 Unhappy flow

- None

# 5. Invalid message header

If the client sends an invalid message header (not defined above), the server replies with an unknown command message.
The client remains connected.

Example:

```
C -> S: MSG This is an invalid message
S -> C: UNKNOWN_COMMAND
```

# 6. Invalid message body

If the client sends a valid message, but the body is not valid JSON, the server replies with a pars error message. The
client remains connected.

Example:

```
C -> S: BROADCAST_REQ {"aaaa}
S -> C: PARSE_ERROR
```

# 7. Request of the list of all connected clients

Requests the list of all connected clients. The server responds with a list of all connected clients.

## 7.1 Happy flow

```
C -> S: USERLIST_REQ
S -> C: USERLIST {"users":["<username1>", "<username2>", ...]}
```

## 7.2 Unhappy flow

- None

# 8. Direct (private) message between clients

Sends a message from a client to another specific client. Other clients do not receive the message.

## 8.1 Happy flow

```
C -> S: DM_REQ {"recipient":"<recipient_username>","message":"<message>"}
S -> C: DM_RESP {"status":"OK"}
```

- `<message>`: the message that must be sent.
- `<recipient_username>`: the username of the user that must receive the message.

Specified client receives the message as follows:

```
S -> C: DM {"sender":"<sender_username>","message":"<message>"}   
```   

- `<sender_username>`: the username of the user that is sending the message.

## 8.2 Unhappy flow

```
S -> C: DM_RESP {"status": "ERROR", "code": <error code>}
```

Possible `<error code>`:

| Error code | Description           |
|------------|-----------------------|
| 4000       | User is not logged in |
| 4001       | Recipient not found   |

# 9. Rock paper scissors game

Start a game of rock paper scissors with another client. The server responds with the result of the game.

## 9.1 Happy flow

Client sends a request to start a game of rock paper scissors with another client.

- Client that sends the RPS request: **C1**
- Client that was challenged to play the game: **C2**

### C1

```
C1 -> S: RPS_REQ {"opponent":"<C2_username>","choice":"<choice>"}
```

Client receives the result of the game as follows:

```
S -> C1: RPS_RESULT {"status":"OK","game_result":"<game_result>","opponent_choice":"<choice>"}
```

### C2

"Opponent" client receives the message as follows:

```
S -> C2: RPS {"opponent":"<C1_username>"}
``` 

"Opponent" client sends a response to the game request as follows:

```
C2 -> S: RPS_RESP {"choice":"<choice>"}
```

"Opponent" client receives the result of the game as follows:

```
S -> C2: RPS_RESULT {"status":"OK","game_result":"<game_result>","opponent_choice":"<choice>"}
```

- `<C1_username>`: the username of the user that sends the RPS request.
- `<C2_username>`: the username of the user that is challenged to play the game.
- `<choice>`: the choice of the user.
- `<game_result>`: the outcome of the game according to the rules.

Possible `<choice>`:

| Choice | Description                               |
|--------|-------------------------------------------|
| 0      | **Rock**, beats scissors, looses to paper |
| 1      | **Paper**, beats rock, looses to scissors |
| 2      | **Scissors**, beats paper, losses to rock |

Possible `<game_result>`:

| Game result | Description                                         |
|-------------|-----------------------------------------------------|
| 0           | The client that had started the game (C1) has won   |
| 1           | The client that was challenged to play (C2) has won |
| 2           | The game ended with draw                            |

## 9.2 Unhappy flow

Depending on the error code, different client participating in the game receives the following message:

```
S -> C: RPS_RESULT {"status": "ERROR", "code": <error code>}
```

In case of the error code 3001:

```
S -> C: RPS_RESULT {"status": "ERROR", "code": <error code>, "now_playing":"{[<username1>, <username2]>}"}
```

- `<username1>, <username2>`: the usernames of the clients that are currently playing the game.

Possible `<error code>`:

- Client that sends the RPS request: **C1**
- Client that receives the message: **C2**

| Error code | Description                                                                      | Client that receives the message |
|------------|----------------------------------------------------------------------------------|----------------------------------|
| 3000       | The client that sends an RPS request is not logged in.                           | C1                               |
| 3001       | There is already an RPS game going on on the server.                             | C1                               |
| 3002       | The client that sends an RPS request specified non existent opponent's username. | C1                               |
| 3003       | The client sent a request, specifying himself as an opponent.                    | C1                               |
| 3004       | The client that sends an RPS request specified incorrect choice code.            | C1                               |
| 3005       | The client that receives an RPS specified incorrect choice code.                 | C1 & C2                          |

# 10. File transfer

Send a file from one client to another. Client requests to send a file to another client. If the recipient exists and
accepts the file, the server will send the file to the recipient.

- Client that sends the file: **C1**
- Client that receives the file: **C2**

## 10.1 Happy flow

### Text based communication, port 1337

C1 sends a request to send a file:

```
C1 -> S: FILE_REQ {"recipient":"<C2_username>",filename":"<filename>","hash":"<hash>"}
```

`<C2_username>` - the username of the recipient of the file.
`<filename>` - the name of the file.
`<hash>` - checksum of the file, calculated with the SHA-256 algorithm.

Server sends a request to C2 accept the file:

```
S -> C2: FILE {"sender":"<C1_username>","filename":"<filename>","hash}
```

C2 sends a response, if the transfer was accepted.
C2 has to include the sender's username in the response to let server know which client one responds to.

```
C2 -> S: FILE_RESP {"sender":"<C1_username>","status":"OK"}
```

C2 -> S FILE_RESP status options: 

- OK - the transfer was accepted.
- ERROR - the transfer was rejected.

Code options: 

- 0 - OK, transfer accepted (can be omitted)
- 9003 - transfer rejected

The FILE_RESP that indicated rejection of the file transfer should look like this:

```
C2 -> S: FILE_RESP {"sender":"<C1_username>","status":"ERROR","code":9003}
```

`<C1_username>` - the username of the sender of the file.

Server sends the response to C1, if the transfer was accepted.
Server include the recipient's username in the response to let C1 know which client has responded.

```
S -> C1: FILE_RESP {"recipient":"<C2_username>","status":"OK"}
```

Server assigns UUID to these two clients and sends it to both of them:

```
S -> C1: FILE_UUID {"uuid":"<uuid>"}
S -> C2: FILE_UUID {"uuid":"<uuid>"}
```

`<uuid>` - the unique identifier for the file transfer.

### File transfer socket, port 1338

Clients send uuids to the server to establish a connection and authenticate themselves.

C1 sends the uuid through the file transfer socket:

```
C1 -> S: <uuid>_send
```

C2 sends the uuid through the file transfer socket:

```
C2 -> S: <uuid>_receive
```

`<uuid>` - the unique identifier for the file transfer.

C1 and C2 need to include an ending to the uuid to let the server know which client is the sender and which is the
receiver. The ending is `_send` for C1 and `_receive` for C2.

Then C1 sends the bytes of the file to the server and server transfers them to C2:

```
C1 -> S: file_bytes
S -> C2: file_bytes
```

## 10.2 Unhappy flow

### Text based communication, port 1337

Client sends a request to send a file:

```
C1 -> S: FILE_REQ {"recipient":"<C2_username>",filename":"<filename>","hash":"<hash>"}
```

`<C2_username>` - the username of the recipient of the file.
`<filename>` - the name of the file.
`<hash>` - checksum of the file, calculated with the SHA-256 algorithm.

Server responds with an error message:

```
S -> C1: FILE_RESP {"status":"ERROR", "code": <error code>}
```

Possible `<error code>`:

| Error code | Description                                                                               |
|------------|-------------------------------------------------------------------------------------------|
| 9000       | The client that sends a file request is not logged in.                                    |
| 9001       | The client that sends a file request specified non existent recipient's username.         |
| 9002       | The client that sends a file request specified himself as the recipient.                  |
| 9003       | The client that receives the file rejected the file transfer.                             |
| 9004       | The client sent a FILE_RESP that contains a sender which did not request a file transfer. |
















