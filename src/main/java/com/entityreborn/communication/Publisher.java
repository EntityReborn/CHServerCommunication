package com.entityreborn.communication;

import com.entityreborn.communication.Exceptions.InvalidChannelException;
import com.entityreborn.communication.Exceptions.InvalidNameException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.simple.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQException;

public class Publisher extends NodePoint implements Runnable {
    private final String publisherId;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<String>();;

    public Publisher(String id) {
        if (id.contains("\0")) {
            throw new IllegalArgumentException("Cannot use \\0 in a publishers ID!");
        }
        
        publisherId = id;
        owningThread = new Thread(this, "publisher-" + publisherId);
    }
    
    public void init(Context context) {
        super.init(context, ZMQ.PUB);
        socket.setIdentity(publisherId.getBytes());
    }
    
    public void publish(String channel, String message) throws InvalidChannelException {
        String chan = channel.trim();
        
        if(!Util.isValidChannel(channel)) {
            throw new InvalidChannelException(channel);
        }
        
        String tosend;
        if (NodePoint.DataStructureType == DataType.Json) {
            JSONObject obj = new JSONObject();
            
            obj.put("channel", chan);
            obj.put("publisherid", publisherId);
            obj.put("message", message);
            
            tosend = obj.toJSONString();
        } else {
            tosend = chan + '\0' + publisherId + '\0' + message;
        }
        
        queue.add(tosend);
    }
    
    public void publish(String channel, String message, String origpub) throws InvalidNameException, InvalidChannelException {
        String chan = channel.trim();
        
        if(!Util.isValidName(origpub)) {
            throw new InvalidNameException(origpub);
        }
        
        if(!Util.isValidChannel(channel)) {
            throw new InvalidChannelException(channel);
        }
        
        String tosend = chan + '\0' + origpub + '\0' + message;
        queue.add(tosend);
    }
    
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && alive) {
            String tosend;
            
            try {
                tosend = queue.take(); // Blocking
            } catch (InterruptedException ex) {
                // something derped, die.
                break;
            }
            
            if (tosend != null) {
                try {
                    socket.send(tosend, 0);
                } catch (ZMQException e) {
                    // something derped, die.
                    break;
                }
            }
        }
        
        cleanup();
    }
    
    public static void main(String[] args) throws InterruptedException, InvalidChannelException {
        NodePoint.DataStructureType = DataType.Json;
        Context context = ZMQ.context(1);
        
        Publisher pub = new Publisher("weather");
        pub.init(context);
        pub.listen("tcp://*:5556");
        
        pub.start();
        
        for (int i=0; i < 50; i++) {
            System.out.println("Publishing " + i);
            pub.publish("SomeChannel", "{12\'\"3#$#*somedata " + i);
            Thread.sleep(1000);
        }
        
        pub.stop();
        
        context.term();
    }
}