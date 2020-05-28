package client;

import client.Message.Message_Type;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Scanner;

/**
 *
 * @author Bedirhan Yıldırım | bedirhan.yildirim@stu.fsm.edu.tr
 * @description 
 * @file Client.java
 * @assignment Computer System Security Programming Assignment
 * @date 18.05.2020
 * 
 */
public class Client {
    
    private Socket socket;
    private String username = null;
    
    private ObjectInputStream clientInput;
    private ObjectOutputStream clientOutput;
    
    private Thread clientThread;
    
    private PublicKey publicKey;
    private PrivateKey privateKey;
    
    // bedo style
    protected void start (String host, int port) throws IOException, NoSuchAlgorithmException {
        System.out.println("Welcome");
        
        // Create client socket (ip + port)
        socket = new Socket(host, port);

        // input  : reading message comes to client
        // output : sending message to server
        clientOutput = new ObjectOutputStream(socket.getOutputStream());
        clientInput = new ObjectInputStream(socket.getInputStream());
        
        generateKeys();
        
        // server'ı sürekli dinlemek için Thread oluştur
        clientThread = new ListenThread();
        clientThread.start();
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Write '-help' for commands");
        System.out.print("Please set your username: ");
        end:
        while (scanner.hasNextLine()) {
            // konsoldan mesaj oku
            Message mess;
            String mesaj = scanner.nextLine();
            
            if (username == null) {
                setUsername(mesaj);
            } else {
            
                switch (mesaj) {
                    case "-end":
                        mess = new Message(Message_Type.Disconnect);
                        Send(mess);
                        disconnect();
                        break end;
                        
                    case "-list":
                        mess = new Message(Message_Type.ClientList);
                        Send(mess);
                        break;

                    case "-help":
                        System.out.println("-help: list of commands");
                        System.out.println("-list: list of online users");
                        System.out.println("-end: close the app");
                        break;

                    default:
                        System.out.println("Invalid command, write '-help' for commands");
                }
            }
        }
    }
    
    private void generateKeys () throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstanceStrong();
        
        keyPairGenerator.initialize(2048, random);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        
        System.out.println("Public key: " + publicKey.toString());
        System.out.println("Private key: " + privateKey.toString());
    }
    
    private void setUsername (String username) throws IOException {
        this.username = username;
        Message msg = new Message(Message_Type.Username, username);
        Send(msg);
    }
    
    private void Send (Message msg) throws IOException {
        this.clientOutput.writeObject(msg);
    }
    
    private void disconnect() throws IOException {
        // bütün streamleri ve soketleri kapat
        if (clientInput != null) {
            clientInput.close();
        }
        if (clientOutput != null) {
            clientOutput.close();
        }
        if (clientThread != null) {
            clientThread.interrupt();
        }
        if (socket != null) {
            socket.close();
        }
    }
    
    private class ListenThread extends Thread {
        
        // server'dan gelen mesajları dinle
        @Override
        public void run() {
            try {
                Object mesaj;
                // server mesaj gönderdiği sürece gelen mesajı al
                while ((mesaj = clientInput.readObject()) != null) {
                    // serverdan gelen mesajı arayüze yaz
                    System.out.println(mesaj);

                    // "son" mesajı iletişimi sonlandırır
                    if (mesaj.equals("end")) {
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Error - ListenThread : " + ex);
            }
        }
        
    }
}
