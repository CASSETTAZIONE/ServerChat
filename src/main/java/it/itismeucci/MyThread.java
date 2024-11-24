package it.itismeucci;

import java.io.*;
import java.net.*;
import java.util.*;

public class MyThread extends Thread {
    private final Socket clientSocket;
    private final Map<String, Socket> userMap;

    public MyThread(Socket socket, Map<String, Socket> userMap) {
        this.clientSocket = socket;
        this.userMap = userMap;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            String nickname = registerNickname(in, out);
            String message;                 // Gestione della comunicazione
            while ((message = in.readLine()) != null) {
                handleCommand(message, out, nickname,in);
            }
        } catch (IOException e) {
            System.err.println("Errore nella comunicazione con il client: " + e.getMessage());
        } finally {
            disconnectClient();
        }
    }

    private void handleCommand(String command, DataOutputStream out, String nickname, BufferedReader in) throws IOException {
        char commandType = command.charAt(0);
        System.out.println(command);
        switch (commandType) {
            case '!': // Disconnessione
                clientSocket.close();
            break;
            case '@':  // Cambio nickname      // Client : /change newUser --> Server : @newUser
            String newUser = command.substring(1).trim();
                synchronized (userMap) {
                    if (!newUser.isEmpty() && !userMap.containsKey(newUser)) {
                        userMap.remove(nickname);
                        userMap.put(newUser, clientSocket);
                        nickname = newUser;
                        out.writeBytes("OK\n");
                    } else {
                        out.writeBytes("CHA\n"); // Nickname già in uso
                    }
                }
            break;
            case '*':
                synchronized (userMap) {
                    out.writeBytes(String.join(",", userMap.keySet()) + "\n");   //utente1,utente2,utente3
                    out.flush();
                    out.writeBytes("\n");
                }
            break;
            case '#':   // Client : /msg user mioMessaggio --> Server : #user_mioMessaggio_ (il messaggio specificato arriverà solamente alla persona specificata)
                String[] parts = command.substring(1).split("_", 2);
                if (parts.length == 2) {
                    String receiver = parts[0];
                    String privMessage = parts[1];
                    synchronized (userMap) {
                        if (userMap.containsKey(receiver)) {
                            Socket receiverSocket = userMap.get(receiver);
                            new DataOutputStream(receiverSocket.getOutputStream()).writeBytes("*PRIVATO*" + nickname + ": " + privMessage + "\n");
                        } else {
                            out.writeBytes("nf\n"); //not found
                        }
                    }
                }
            break;
            default:            //la gestione di comandi non esistenti è già gestita lato client
                String message = "";
                for (int i = 0; i < command.length(); i++) {
                    message += command.charAt(i);
                }
                broadcastMessage(nickname,message);
        }
    }

private String registerNickname(BufferedReader in, DataOutputStream out) throws IOException {
        String nickname;
        do {
            nickname = in.readLine().trim();
            synchronized (userMap) {
                if (!nickname.isEmpty() && !userMap.containsKey(nickname)) {
                    userMap.put(nickname, clientSocket);
                    out.writeBytes("REG\n");
                    break;
                } else {
                    out.writeBytes("CHA\n"); // Nickname già in uso o vuoto
                }
            }
        } while (true);
        return nickname;
    }


    private void broadcastMessage(String sender, String message) {
        synchronized (userMap) {
            userMap.forEach((nickname, socket) -> {
                if (!socket.equals(clientSocket)) {
                    try {
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeBytes(sender + ": " + message + "\n");
                        out.flush();
                    } catch (IOException e) {
                        System.err.println("Errore durante l'invio del messaggio a " + nickname + ": " + e.getMessage());
                    }
                }
            });
        }
    }
    

    private void disconnectClient() {
        synchronized (userMap) {
            userMap.values().removeIf(socket -> socket.equals(clientSocket));
        }
        System.out.println("Client disconnesso");
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura del socket: " + e.getMessage());
        }
    }
}