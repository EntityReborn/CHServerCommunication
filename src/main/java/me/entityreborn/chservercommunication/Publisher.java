package me.entityreborn.chservercommunication;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

public class Publisher extends NodePoint implements Runnable {
    private String publisherId;
    private BlockingQueue<String> queue;

    public Publisher(String id) {
        if (id.contains("\0")) {
            throw new IllegalArgumentException("Cannot use \\0 in a publishers ID!");
        }
        
        publisherId = id;
        queue = new LinkedBlockingQueue<String>();
        owningThread = new Thread(this, "publisher-" + publisherId);
    }
    
    public void init(Context context) {
        super.init(context, ZMQ.PUB);
        socket.setIdentity(publisherId.getBytes());
    }
    
    public void publish(String channel, String string) {
        String chan = channel.trim();
        
        if (chan.contains("\0")) {
            throw new IllegalArgumentException("Cannot publish to channels with \\0 in them!");
        }
        
        String tosend = chan + '\0' + publisherId + '\0' + string;
        queue.add(tosend);
    }
    
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && alive) {
            String tosend;
            
            try {
                tosend = queue.take(); // Blocking
            } catch (InterruptedException ex) {
                break;
            }
            
            if (tosend != null) {
                socket.send(tosend, 0);
            }
        }
        
        cleanup();
    }
    
    public static void main(String[] args) throws InterruptedException {
        Context context = ZMQ.context(1);
        
        Publisher pub = new Publisher("weather");
        pub.init(context);
        pub.listen("tcp://*:5556");
        
        pub.start();
        
        for (int i=0; i < 50; i++) {
            System.out.println("Publishing " + i);
            pub.publish("weather1", "somedata " + i);
            Thread.sleep(1000);
        }
        
        pub.stop();
        
        context.term();
    }
}