package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
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
    
    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 5000;

        new Client().start(host, port);
    }
    
    private void start(String host, int port) throws IOException {
        // Create client socket (ip + port)
        Socket socket = new Socket(host, port);

        // input  : reading message comes to client
        // output : sending message to server
        Scanner input = new Scanner(socket.getInputStream());
        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

        // Send message to server
        System.out.print("Type : ");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            // Read console
            String message = scanner.nextLine();
            // Send message
            output.println(message);
            // Print message to console comes from server
            System.out.println(input.nextLine());

            // "end" ends messaging
            if (message.equals("end")) {
                break;
            }

            System.out.print("Type : ");
        }
    }
}
