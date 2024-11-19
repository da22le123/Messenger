import model.Client;

import java.io.*;
import java.util.Scanner;

public class Main {
    static final String SERVER_IP = "localhost";
    static final int SERVER_PORT = 1337;

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        Client client = new Client(SERVER_IP, SERVER_PORT);

        while (true) {
            selectOption(sc, client);
        }

    }

    public static void printMenu() {
        System.out.println("1. Start chatting");
        System.out.println("2. Exit");
    }

    public static void selectOption(Scanner sc, Client client) throws InterruptedException, IOException {
        int option = -1;

        while (option < 1 || option > 3) {
            System.out.println("Select an option: ");
            printMenu();
            option = sc.nextInt();
        }

        switch (option) {
            case 1: {
                client.startChatting();
                break;
            }
            case 2: {
                client.exit();
                break;
            }
            default:
                System.out.println("Invalid option");
        }
    }

    // should not Client be a runnable? We need multiple clients to communicate with each other so
    // how do we run them multiple times


}