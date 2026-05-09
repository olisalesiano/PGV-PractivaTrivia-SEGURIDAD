package net.salesianos.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import net.salesianos.client.threads.ServerListener;
import net.salesianos.utils.Constants;
import net.salesianos.utils.CryptoUtils;

public class ClientApp {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Introduce tu nombre de usuario:");
        String name = scanner.nextLine();

        Socket socket = new Socket("localhost", Constants.SERVER_PORT);

        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        outputStream.writeUTF(CryptoUtils.encrypt(name));
        outputStream.flush();

        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        ServerListener serverListenerThread = new ServerListener(inputStream);
        serverListenerThread.start();

        System.out.println("Conectado. Esperando jugadores...");

        while (true) {
            if (socket.isClosed())
                break;
            System.out.print("-> ");
            String message = scanner.nextLine();

            if (message.equalsIgnoreCase("exit")) {
                outputStream.close();
                inputStream.close();
                socket.close();
                break;
            }
            outputStream.writeUTF(CryptoUtils.encrypt(message));
            outputStream.flush();
        }
        scanner.close();
    }
}