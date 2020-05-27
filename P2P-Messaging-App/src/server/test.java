package server;

import java.io.IOException;

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
    public static void main(String[] args) throws IOException {
        int port = 5000;

        Server s = new Server();
        s.start(port);
    }
}
