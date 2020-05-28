package client;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Bedirhan Yıldırım | bedirhan.yildirim@stu.fsm.edu.tr
 * @description 
 * @file test.java
 * @assignment Computer System Security Programming Assignment
 * @date 19.05.2020
 * 
 */
public class test {
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String host = "localhost";
        int port = 5000;

        Client c = new Client();
        c.start(host, port);
    }
}
