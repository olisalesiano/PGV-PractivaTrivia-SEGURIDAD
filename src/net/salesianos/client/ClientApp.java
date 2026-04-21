package net.salesianos.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

import net.salesianos.client.threads.ServerListener;
import net.salesianos.utils.Constants;

public class ClientApp {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Introduce tu nombre de usuario:");
        String name = scanner.nextLine();

        Socket socket = new Socket("localhost", Constants.SERVER_PORT);

        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        outputStream.writeUTF(name);
        outputStream.flush();

        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        ServerListener serverListenerThread = new ServerListener(inputStream);
        serverListenerThread.start();

        while (true) {
            try {
                System.out.print("-> ");
                String message = scanner.nextLine();

                if (message.contains("exit")) {
                    outputStream.close();
                    inputStream.close();
                    socket.close();
                }
                outputStream.writeUTF(message);
                outputStream.flush();
            } catch (InputMismatchException e) {
                System.out.println("Esto no es un número, crema.");
                scanner.nextLine();
            }
        }
    }
}
