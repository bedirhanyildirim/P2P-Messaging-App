package client;

/**
 *
 * @author Bedirhan Yıldırım | bedirhan.yildirim@stu.fsm.edu.tr
 * @description 
 * @file Message.java
 * @assignment Computer System Security Programming Assignment
 * @date 19.05.2020
 * 
 */
public class Message implements java.io.Serializable {
    
    public static enum Message_Type {
        Connect, Disconnect, Warning, Message, Username, End, ClientList, PublicKey, Pair, Nonce, askPublicKey, Confirm
    }
    
    private Message_Type type;
    private Object content;
    private String to;
    
    public Message (Message_Type t) {
        this.type = t;
        this.content = null;
    }
    
    public Message (Message_Type t, Object o) {
        this.type = t;
        this.content = o;
    }
    
    public Message (Message_Type t, Object o, String to) {
        this.type = t;
        this.content = o;
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public Message_Type getType() {
        return type;
    }

    public Object getContent() {
        return content;
    }
    
    @Override
    public String toString() {
        String result = "Type: " + this.type + ", Content: " + this.content;
        return result;
    }
}
