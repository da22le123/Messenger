package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class FileTransferManager {
    // map <UUID, map <receiver, sender>>
    private final HashMap<String, HashMap<OutputStream, InputStream>> transfers = new HashMap<>();

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
     * Check if the transfer is ready to start by checking if both sender and receiver are set
     */
    public synchronized boolean isTransferReady(String uuid) {
        // Grab the sub-map in one shot
        HashMap<OutputStream, InputStream> map = transfers.get(uuid);

        // If there's no sub-map or it's empty, definitely not ready
        if (map == null || map.isEmpty()) {
            return false;
        }

        // Since there's supposed to be only one entry, let's get that one
        Map.Entry<OutputStream, InputStream> entry = map.entrySet().stream()
                .findFirst()
                .orElse(null);

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
        try (InputStream in = getSender(uuid);
             OutputStream out = getReceiver(uuid))
        {
            in.transferTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
