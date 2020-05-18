package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

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

    public static void main(String[] args) throws IOException {
        // initialize the port
        int port = 5000;
        // start server
        new Server().start(port);
    }
    
    private void start(int port) throws IOException {
        ServerSocket socket = new ServerSocket(port);
        System.out.println("Server started..");
        
        while (true) {
            System.out.println("Waiting for new client..");
            Socket clientSocket = socket.accept();
            System.out.println("New client connected: " + clientSocket);
            new ListenThread(clientSocket).start();
        }
    }
    
    class ListenThread extends Thread {
        
        private final Socket socket;
        
        private ListenThread (Socket clientSocket) {
            this.socket = clientSocket;
        }
        
        @Override
        public void run () {
            System.out.println("Thread created for connected client: " + this.getName());
            
            try {
                // input : reading message comes from client
                // output: sending message to client
                Scanner input = new Scanner(socket.getInputStream());
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                
                // wait the client for sending message
                while (input.hasNextLine()) {
                    // comming message
                    String message = input.nextLine();
                    System.out.println(this.getName() + " : " + message);
                    
                    // sending message
                    output.println("Received: " + message);
                    
                    // "end" ends messaging
                    if (message.equals("end")) {
                        break;
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error: " + ex);
            } finally {
                try {
                    socket.close();
                    System.out.println("Socked ended: " + socket);
                } catch (IOException ex) {
                    System.out.println("Soked couldn't ended: " + ex);
                }
            }
        }
    }
}
