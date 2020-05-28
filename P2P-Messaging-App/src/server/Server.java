package server;

import client.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Bedirhan Yıldırım | bedirhan.yildirim@stu.fsm.edu.tr
 * @description 
 * @file Server.java
 * @assignment Computer System Security Programming Assignment
 * @date 18.05.2020
 * 
 */
public class Server {
    
    private ServerSocket serverSocket;
    private Thread serverThread;
    private HashSet<ObjectOutputStream> allClients = new HashSet<>();
    private ArrayList<Client> clients = new ArrayList<>();
    
    protected  void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started..");
        
        serverThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    // blocking call, yeni bir client bağlantısı bekler
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Yeni bir client bağlandı : " + clientSocket);

                    // bağlanan her client için bir thread oluşturup dinlemeyi başlat
                    new ListenThread(clientSocket).start();
                } catch (IOException ex) {
                    System.out.println("Hata - new Thread() : " + ex);
                    break;
                }
            }
        });
        serverThread.start();
    }
    
    private String listOfClients() {
        // bütün client isimlerini gönderir
        String names = "Users: ";
        
        for (Client c : clients) {
            names += c.getUsername() + ", ";
        }
        
        return names;
    }
    
    private void stop() throws IOException {
        // bütün streamleri ve soketleri kapat
        if (serverSocket != null) {
            serverSocket.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
    
    private class ListenThread extends Thread {
        
        private final Socket clientSocket;
        private ObjectInputStream clientInput;
        private ObjectOutputStream clientOutput;
        private String username;
        private Client me;
        
        private ListenThread (Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run () {
            System.out.println("Thread created for connected client: " + this.getName());
            
            try {
                // input : reading message comes from client
                // output: sending message to client
                clientInput = new ObjectInputStream(clientSocket.getInputStream());
                clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
                
                // broadcast için, yeni gelen client'ın output stream'ını listeye ekler
                allClients.add(clientOutput);
                
                // client ismini mesaj olarak gönder
                //clientOutput.writeObject("@id-" + this.getName());
                
                Message mesaj;
                // client mesaj gönderdiği sürece mesajı al
                end:
                while ((mesaj = (Message)clientInput.readObject()) != null) {
                    // client'in gönderdiği mesajı server ekranına yaz
                    System.out.println(this.username + " : " + mesaj);
                    
                    switch (mesaj.getType()) {
                        case Username:
                            this.username = mesaj.getContent().toString();
                            me = new Client(clientSocket, username, clientInput, clientOutput);
                            clients.add(me);
                            System.out.println("Added to clients list.");
                            break;
                            
                        case ClientList:
                            String list = listOfClients();
                            Message msg = new Message(Message.Message_Type.ClientList, list);
                            clientOutput.writeObject(msg);
                            break;
                            
                        case Disconnect:
                            break end;
                            
                        default:
                            break;
                    }

                    // "end" mesajı iletişimi sonlandırır
                    if (mesaj.equals("end")) {
                        break;
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error: " + ex);
            } catch (ClassNotFoundException ex) {
                System.out.println("Error: " + ex);
            } finally {
                try {
                    // client'ların tutulduğu listeden çıkart
                    allClients.remove(clientOutput);
                    clients.remove(me);

                    // bütün streamleri ve soketleri kapat
                    if (clientInput != null) {
                        clientInput.close();
                    }
                    if (clientOutput != null) {
                        clientOutput.close();
                    }
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                    System.out.println("Soket kapatıldı : " + clientSocket);
                } catch (IOException ex) {
                    System.out.println("Hata - Soket kapatılamadı : " + ex);
                }
            }
        }
    }
    
    private class Client {
        
        private final Socket clientSocket;
        private String username;
        private ObjectInputStream clientInput;
        private ObjectOutputStream clientOutput;
        private PublicKey publicKey;
                
        public Client (Socket socket, String username, ObjectInputStream clientInput, ObjectOutputStream clientOutput) {
            this.clientSocket = socket;
            this.username = username;
            this.clientInput = clientInput;
            this.clientOutput = clientOutput;
        }
        
        Socket getSocket () {
            return clientSocket;
        }
        
        String getUsername () {
            return username;
        }
        
        ObjectInputStream getObjectInputStream () {
            return clientInput;
        }
        
        ObjectOutputStream getObjectOutputStream () {
            return clientOutput;
        }
        
        PublicKey getPublicKey () {
            return publicKey;
        }
    }
}
