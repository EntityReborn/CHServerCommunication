/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.chservercommunication;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 *
 * @author import
 */
public class NodePoint {
    protected Socket socket;
    private boolean isInited = false;
    protected Thread owningThread;
    protected boolean alive = true;
    protected int type;

    protected void cleanup() {
        try {
            socket.setLinger(1);
            socket.close();
        } catch (Exception e) {
            Logger.getLogger(NodePoint.class.getName()).log(Level.WARNING, "Exception while closing node:", e);
        }
        
        System.out.println("Closed!");
    }
    
    public boolean isAlive() {
        return alive;
    }

    public void connect(String endpoint) {
        socket.setLinger(-1);
        socket.connect(endpoint);
    }

    public void disconnect(String endpoint) {
        socket.disconnect(endpoint);
    }

    public void listen(String endpoint) {
        socket.setLinger(-1);
        socket.bind(endpoint);
    }
    
    public boolean isInitialized() {
        return isInited;
    }

    protected void init(Context context, int type) {
        this.type = type;
        
        socket = context.socket(type);
        isInited = true;
    }
    
    public void start() {
        owningThread.start();
    }
    
    public void stop() {
        alive = false;
        
        if (this instanceof Publisher) {
            owningThread.interrupt();
        }
    }
}
