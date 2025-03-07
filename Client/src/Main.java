import model.Client;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    static final String SERVER_IP = "localhost";
    static final int SERVER_PORT = 1337;

    public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException {
        Scanner sc = new Scanner(System.in);
        Client client = new Client(SERVER_IP, SERVER_PORT);

        while (true) {
            selectOption(sc, client);
        }
    }

    /**
     * Prints the menu
     */
    public static void printMenu() {
        System.out.println("1. Start chatting");
        System.out.println("2. Request list of all connected users");
        System.out.println("3. Play rock-paper-scissors");
        System.out.println("4. Play tic-tac-toe");
        System.out.println("5. Send a file");
        System.out.println("6. Exit");
    }

    /**
     * Selects an option from the menu
     * @param sc Scanner object
     * @param client Client object
     * @throws InterruptedException
     * @throws IOException
     */
    public static void selectOption(Scanner sc, Client client) throws InterruptedException, IOException, NoSuchAlgorithmException {
        int option = -1;

        do {
            System.out.println("Select an option: ");
            printMenu();
            try {
                option = sc.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input");
                sc.nextLine();
            }
        } while (option < 1 || option > 6);

        switch (option) {
            case 1: {
                client.startChatting();
                break;
            }
            case 2: {
                client.requestUserList();
                break;
            }
            case 3: {
                client.startRpsGame();
                break; 
            }
            case 4: {
                client.startTicTacToeGame();
                break;
            }
            case 5: {
                client.sendFile();
                break;
            }
            case 6: {
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