package it.itismeucci;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerChat {
    public static final int PORT = 3645;
    private static final Map<String, Socket> userMap = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server avviato sulla porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso");
                new MyThread(clientSocket,userMap).start();
            }
        } catch (IOException e) {
            System.err.println("Errore: " + e.getMessage());
        }
    }
}