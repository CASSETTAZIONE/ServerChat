package it.itismeucci;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerChat {
    public static final int PORT = 3645;
    private static final Map<Socket, String> userMap = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server avviato sulla porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso");
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Errore avviando il server: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                // Assegna un nickname iniziale
                out.writeBytes("Inserisci il tuo nickname:\n");
                String nickname = in.readLine();
                synchronized (userMap) {
                    while (userMap.containsValue(nickname)) {
                        out.writeBytes("Nickname già in uso. Scegline un altro:\n");
                        nickname = in.readLine();
                    }
                    userMap.put(clientSocket, nickname);
                }
                System.out.println("Utente registrato con nickname: " + nickname);

                // Comunica con il client
                String message;
                while ((message = in.readLine()) != null) {
                    switch (message) {
                        case "/quit":
                            out.writeBytes("Disconnessione...\n");
                            clientSocket.close();
                            synchronized (userMap) {
                                userMap.remove(clientSocket);
                            }
                            return;

                        case "/change":
                            out.writeBytes("Inserisci il nuovo nickname:\n");
                            String newNickname = in.readLine();
                            synchronized (userMap) {
                                while (userMap.containsValue(newNickname)) {
                                    out.writeBytes("Nickname già in uso. Scegline un altro:\n");
                                    newNickname = in.readLine();
                                }
                                userMap.put(clientSocket, newNickname);
                            }
                            out.writeBytes("Nickname cambiato in: " + newNickname + "\n");
                            break;

                        case "/help":
                            out.writeBytes("Comandi disponibili:\n" +
                                    "/quit - Disconnessione\n" +
                                    "/change - Cambia nickname\n" +
                                    "/list - Mostra utenti connessi\n" +
                                    "/msg <nickname> <messaggio> - Invia un messaggio privato\n" +
                                    "/pb - Ritorna alla chat pubblica\n");
                            break;

                        case "/list":
                            synchronized (userMap) {
                                out.writeBytes("Utenti connessi: " + String.join(", ", userMap.values()) + "\n");
                            }
                            break;

                        case "/pb":
                            out.writeBytes("Tornato alla chat pubblica\n");
                            break;

                        default:
                            if (message.startsWith("/msg ")) {
                                String[] parts = message.split(" ", 3);
                                if (parts.length < 3) {
                                    out.writeBytes("Uso corretto: /msg <nickname> <messaggio>\n");
                                } else {
                                    String targetNickname = parts[1];
                                    String privateMessage = parts[2];
                                    boolean found = false;
                                    synchronized (userMap) {
                                        for (Map.Entry<Socket, String> entry : userMap.entrySet()) {
                                            if (entry.getValue().equals(targetNickname)) {
                                                new DataOutputStream(entry.getKey().getOutputStream()).writeBytes("[Privato da " + nickname + "] " + privateMessage + "\n");
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!found) {
                                        out.writeBytes("Utente non trovato\n");
                                    }
                                }
                            } else {
                                // Messaggio pubblico
                                synchronized (userMap) {
                                    for (Socket socket : userMap.keySet()) {
                                        if (socket != clientSocket) {
                                            new DataOutputStream(socket.getOutputStream()).writeBytes(nickname + ": " + message + "\n");
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Errore nella comunicazione con il client: " + e.getMessage());
            } finally {
                synchronized (userMap) {
                    userMap.remove(clientSocket);
                }
                System.out.println("Client disconnesso");
            }
        }
    }
}
