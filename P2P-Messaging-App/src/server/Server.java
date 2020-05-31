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
    
    private Client findClient (String username) {
        for (Client c : clients) {
            if (c.getUsername().equals(username)) {
                return c;
            }
        }
        return null;
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
        
        private Client pair;
        
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
                Message msg;
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
                            
                        case PublicKey:
                            me.setPublicKey((PublicKey) mesaj.getContent());
                            break;
                            
                        case ClientList:
                            String list = listOfClients();
                            msg = new Message(Message.Message_Type.ClientList, list);
                            clientOutput.writeObject(msg);
                            break;
                            
                        case Pair:
                            Client to = findClient((String) mesaj.getContent());
                            if (to != null) {
                                pair = to;
                                System.out.println("to: " + to.getUsername());
                                msg = new Message(Message.Message_Type.Pair, to.getPublicKey());
                                clientOutput.writeObject(msg);
                                System.out.println("Public key: " + to.getPublicKey());
                            } else {
                                System.out.println("Not valid");
                            }
                            break;
                            
                        case Nonce:
                            Object content = mesaj.getContent();
                            msg = new Message(Message.Message_Type.Nonce, content, username);
                            pair.clientOutput.writeObject(msg);
                            break;
                            
                        case askPublicKey:
                            Client asking = findClient((String) mesaj.getContent());
                            if (asking != null) {
                                msg = new Message(Message.Message_Type.askPublicKey, asking.getPublicKey(), (String) mesaj.getContent());
                                clientOutput.writeObject(msg);
                            }
                            break;
                            
                        case Confirm:
                            Client asking2 = findClient((String) mesaj.getContent());
                            Object content2 = mesaj.getContent();
                            if (asking2 != null) {
                                msg = new Message(Message.Message_Type.Confirm, content2, username);
                                clientOutput.writeObject(msg);
                            }
                            break;
                            
                        case Message:
                            
                            break;
                            
                        case Disconnect:
                            break end;
                            
                        default:
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
        
        public void setPublicKey (PublicKey pk) {
            this.publicKey = pk;
        }
        
        PublicKey getPublicKey () {
            return publicKey;
        }
    }
}
