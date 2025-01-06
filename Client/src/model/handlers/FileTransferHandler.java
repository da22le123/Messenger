package model.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import model.messages.receive.File;
import model.messages.receive.FileResponseReceive;
import model.messages.receive.FileUUID;
import utils.CheckSumCalculator;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class FileTransferHandler {
    // hash of the file that is being transferred
    private String currentFileTransferHash;
    // 0 - no file transfer, 1 - this instance is the sender, 2 - this instance is receiver
    private int currentFileTransferStatus;
    // path of the file on the machine of sender
    private String filePathSending;
    // name of the file + extension
    private String fileName;

    private Socket fileTransferSocket;

    private String ipAddress = "localhost";

    private final static int PORT = 1338;


    private final ChatHandler chatHandler;

    public FileTransferHandler(ChatHandler chatHandler, String ipAddress) {
        this.chatHandler = chatHandler;
        currentFileTransferStatus = 0;
    }


    public void handleFile(String payload) throws JsonProcessingException {
        File fileMessage = File.fromJson(payload);
        currentFileTransferStatus = 2;
        chatHandler.addIncomingFileRequest(fileMessage.sender());
        currentFileTransferHash = fileMessage.hash();
        fileName = fileMessage.filename();

        if (chatHandler.isInChat()) {
            System.out.println("You have received a file from " + fileMessage.sender() + ". The file name is " + fileName + ". Do you want to accept it? Type /file_answer (yes/no)");
        } else {
            System.out.println("You have received a file from " + fileMessage.sender() + ". The file name is " + fileName + ". To answer, you have to enter the chat first.");
        }
    }

    public void handleFileResponse(String payload) throws IOException {
        FileResponseReceive fileResponse = FileResponseReceive.fromJson(payload);


        if (fileResponse.status().isOk()) {
            System.out.println("The file transfer has been accepted by recipient.");
        } else {
            int errorCode = fileResponse.status().code();

            switch(errorCode) {
                case 9000:
                    System.out.println("You are not logged in.");
                    break;
                case 9001:
                    System.out.println("You specified non-existent recipient's username.");
                    break;
                case 9002:
                    System.out.println("You cannot send a file to yourself.");
                    break;
                case 9003:
                    System.out.println("The recipient declined the file transfer.");
                    break;
                default:
                    System.out.println("Unknown error.");
            }

            currentFileTransferStatus = 0;
        }
    }

    public void handleFileUUID(String payload) throws IOException, NoSuchAlgorithmException {
        FileUUID fileUUID = FileUUID.fromJson(payload);
        fileTransferSocket = new Socket(ipAddress, PORT);
        PrintWriter fileOut = new PrintWriter(fileTransferSocket.getOutputStream(), true);

        String mode = currentFileTransferStatus == 1 ? "_send" : "_receive";

        fileOut.println(fileUUID.uuid() + mode);
        switch (currentFileTransferStatus) {
            case 1 -> new Thread(() -> sendFile(filePathSending, fileTransferSocket)).start();
            case 2 -> new Thread(this::receiveFile).start();
        }
    }

    public void sendFile(String filePath, Socket fileTransferSocket) {
        System.out.println("Sending file...");
        try (FileInputStream in = new FileInputStream(new java.io.File(filePath));
             OutputStream out = fileTransferSocket.getOutputStream()) {


            // transfer the file directly to the output stream
            in.transferTo(out);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            cleanUpStateOfFileTransfer();
        }
    }

    public void receiveFile() {
        System.out.println("Receiving file...");
        try (InputStream in = fileTransferSocket.getInputStream();
             FileOutputStream fileOut = new FileOutputStream("/Users/illiapavelko/" + fileName)) {


            // Copy the entire stream directly into the file
            in.transferTo(fileOut);
            // fileTransferSocket.close();

            String receivedFileChecksum = CheckSumCalculator.calculateSHA256("/Users/illiapavelko/" + fileName);
            if (currentFileTransferHash.equals(receivedFileChecksum)) {
                System.out.println("File received successfully, the checksum of the file is the same as before sending it.");
            } else {
                System.out.println("File received, but the checksums do not match.");
            }
            // System.out.println("123 file checksum: " + receivedFileChecksum);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }finally {
            cleanUpStateOfFileTransfer();
        }

    }

    public void cleanUpStateOfFileTransfer() {
        currentFileTransferHash = null;
        currentFileTransferStatus = 0;
        filePathSending = null;
    }

    public int getCurrentFileTransferStatus() {
        return currentFileTransferStatus;
    }

    public void setCurrentFileTransferStatus(int currentFileTransferStatus) {
        this.currentFileTransferStatus = currentFileTransferStatus;
    }

    public String getCurrentFileTransferHash() {
        return currentFileTransferHash;
    }

    public void setCurrentFileTransferHash(String currentFileTransferHash) {
        this.currentFileTransferHash = currentFileTransferHash;
    }

    public String getFilePathSending() {
        return filePathSending;
    }

    public void setFilePathSending(String filePathSending) {
        this.filePathSending = filePathSending;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
