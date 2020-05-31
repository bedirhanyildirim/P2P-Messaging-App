package client;

import client.Message.Message_Type;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import server.Server;

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
    
    private String pairUsername;
    private PublicKey pairPublicKey;
    private int nonce;
    private boolean pairOk = false;
    private int replyNonce;
    
    // bedo style
    protected void start (String host, int port) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        System.out.println("Welcome");
        
        // Create client socket (ip + port)
        socket = new Socket(host, port);

        // input  : reading message comes to client
        // output : sending message to server
        clientOutput = new ObjectOutputStream(socket.getOutputStream());
        clientInput = new ObjectInputStream(socket.getInputStream());
        
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
                        
                    case "-messageTo":
                        System.out.print("To: ");
                        String pairUsername = scanner.nextLine();
                        mess = new Message(Message_Type.Pair, pairUsername);
//                        byte[] enc = ecryptData(pairUsername);
//                        decryptData(enc);
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
    
    private void generateKeys () throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstanceStrong();
        
        keyPairGenerator.initialize(2048, random);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
        
        System.out.println("Public key: " + publicKey.toString());
        System.out.println("Private key: " + privateKey.toString());
        
        Message msg = new Message(Message_Type.PublicKey, publicKey);
        Send(msg);
    }
    
    private void setUsername (String username) throws IOException, NoSuchAlgorithmException {
        this.username = username;
        Message msg = new Message(Message_Type.Username, username);
        Send(msg);
        
        // Generate public and private keys
        generateKeys();
    }
    
    private void Send (Message msg) throws IOException {
        this.clientOutput.writeObject(msg);
    }
    
    private byte[] ecryptData (String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] dataToEncrypt = data.getBytes();
        byte[] encryptedData = null;
        
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pairPublicKey);
            encryptedData = cipher.doFinal(dataToEncrypt);
            System.out.println("Encrypted Data: " + encryptedData);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encryptedData;
    }
    
    private Object decryptData (byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] descryptedData = null;
        
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            descryptedData = cipher.doFinal(data);
            System.out.println("Decrypted Data: " + new String(descryptedData));
            return new String(descryptedData);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        return null;
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
    
    private double getRandomIntegerBetweenRange(double min, double max){
        double x = (int)(Math.random()*((max-min)+1))+min;
        return x;
    }
    
    private class ListenThread extends Thread {
        
        // server'dan gelen mesajları dinle
        @Override
        public void run() {
            try {
                Message mesaj;
                // server mesaj gönderdiği sürece gelen mesajı al
                end:
                while ((mesaj = (Message) clientInput.readObject()) != null) {
                    // serverdan gelen mesajı arayüze yaz
                    System.out.println(mesaj);
                    
                    Message msg;

                    switch (mesaj.getType()) {
                        case Pair:
                            pairPublicKey = (PublicKey) mesaj.getContent();
                            nonce = (int) getRandomIntegerBetweenRange(0,10);
                            System.out.println("Random number: " + nonce);
                            byte[] enc = ecryptData(String.valueOf(nonce));
                            msg = new Message(Message_Type.Nonce, enc, pairUsername);
                            Send(msg);
                            break;
                            
                        case Nonce:
                            byte[] checkNonce = (byte[]) mesaj.getContent();
                            replyNonce = Integer.parseInt((String) decryptData(checkNonce));
                            String from = mesaj.getTo();
                            msg = new Message(Message_Type.askPublicKey, from);
                            Send(msg);
                            break;
                            
                        case askPublicKey:
                            pairPublicKey = (PublicKey) mesaj.getContent();
                            pairUsername = (String) mesaj.getTo();
                            byte[] encc = ecryptData(String.valueOf(replyNonce));
                            msg = new Message(Message_Type.Confirm, encc, pairUsername);
                            Send(msg);
                            break;
                            
                        case Confirm:
                            byte[] checkNonce2 = (byte[]) mesaj.getContent();
                            int checkNonce2enc = Integer.parseInt((String) decryptData(checkNonce2));
                            
                            if (checkNonce2enc == nonce) {
                                pairOk = true;
                                System.out.println("Ok, you can message now!");
                            }
                            break;
                        
                        case Disconnect:
                            break end;
                            
                        default:
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Error - ListenThread : " + ex);
            } catch (NoSuchPaddingException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadPaddingException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
}
