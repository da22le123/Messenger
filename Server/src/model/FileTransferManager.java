package model;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FileTransferManager {
    // map <UUID, map <receiver, sender>>
    private final Map<String, HashMap<OutputStream, InputStream>> transfers = new HashMap<>();

    public void getUUIDnUpdateTransfersMap(Socket fileTransferSocket) {
        new Thread(() -> {
            try {
                // Read exactly 39 bytes from the input stream
                byte[] buffer = fileTransferSocket.getInputStream().readNBytes(39);
                String message = new String(buffer, StandardCharsets.UTF_8); // Convert bytes to string
                System.out.println("Received file-transfer identifier: " + message);
                String[] parts = message.split("_");
                if (parts.length < 2) {
                    System.err.println("Invalid file-transfer identifier: " + message);
                    return;
                }

                String uuid = parts[0];
                char mode = parts[1].charAt(0); // "s" or "r"

                if (!hasTransfer(uuid)) {
                    System.err.println("Unknown client: " + uuid);
                    return;
                }

                switch (mode) {
                    case 's':
                        InputStream in = fileTransferSocket.getInputStream();
                        setSender(uuid, in);
                        break;
                    case 'r':
                        OutputStream out = fileTransferSocket.getOutputStream();
                        setReceiver(uuid, out);
                        break;
                    default:
                        System.err.println("Invalid file-transfer mode: " + mode);
                        break;
                }

                if (isTransferReady(uuid)){
                    new Thread(() -> {
                        startTransfer(uuid);
//                        try {
//                            System.out.println("closing file transfer socket");
//                            fileTransferSocket.close();
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
                    }).start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public synchronized void addTransfer(String uuid) {
        transfers.put(uuid, new HashMap<>());
    }

    public synchronized OutputStream getReceiver(String uuid) {
        return transfers.get(uuid).keySet().stream().findFirst().orElse(null);
    }

    public synchronized InputStream getSender(String uuid) {
        return transfers.get(uuid).values().stream().findFirst().orElse(null);
    }

    public synchronized void setSender(String uuid, InputStream sender) {
        OutputStream receiver = getReceiver(uuid);
        transfers.get(uuid).put(receiver, sender);
    }

    public synchronized void setReceiver(String uuid, OutputStream receiver) {
        InputStream sender = getSender(uuid);
        transfers.get(uuid).put(receiver, sender);
    }

    public synchronized boolean hasTransfer(String uuid) {
        return transfers.containsKey(uuid);
    }

    /*
     * Check if the transfer is ready to start (bytes can be transferred from client 1 to client 2)
     * by checking if both sender and receiver are set
     */
    public synchronized boolean isTransferReady(String uuid) {
        // Grab the sub-map in one shot
        HashMap<OutputStream, InputStream> map = transfers.get(uuid);

        // If there's no sub-map or it's empty, definitely not ready
        if (map == null || map.isEmpty()) {
            return false;
        }

        // Since there's supposed to be only one entry, let's get that one
        Map.Entry<OutputStream, InputStream> entry = map.entrySet().stream().findFirst().orElse(null);

        // If for some reason there's no entry, it's not ready
        if (entry == null) {
            return false;
        }

        // It's ready only if both key (BufferedReader) and value (PrintWriter) are non-null
        return entry.getKey() != null && entry.getValue() != null;
    }

    public synchronized void startTransfer(String uuid) {
        System.out.println("starting transfer for " + uuid);
        // Start the transfer
        try (InputStream in = getSender(uuid); OutputStream out = getReceiver(uuid)) {
            in.transferTo(out);
            System.out.println("File is completely transferred.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
