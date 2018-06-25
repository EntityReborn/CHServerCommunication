package com.entityreborn.communication;

import com.laytonsmith.extensions.chsc.Tracking;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zeromq.ZAuth;
import org.zeromq.ZCertStore;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Subscriber extends NodePoint implements Runnable {
    public static interface MessageCallback {
        public void process(String subscriber, String channel, String publisher, String message);
    }
    
    private final Set<MessageCallback> callbacks = Collections.synchronizedSet(new HashSet<MessageCallback>());
    private final String name;

    public Subscriber(String name) {
        owningThread = new Thread(this, "subscriber-" + name);
        this.name = name;
    }
    
    public void init(ZContext context) {
        super.init(context, ZMQ.SUB);
    }
    
    public void addCallback(MessageCallback toRun) {
        callbacks.add(toRun);
    }
    
    public void remCallback(MessageCallback toRun) {
        callbacks.remove(toRun);
    }
    
    private String sanitizeChannel(String channel) {
        String chan = channel.trim();
        
        if (channel.equals("*")) {
            chan = "";
        }
        
        return chan;
    }

    @Override
    public void connect(String endpoint) {
        super.connect(endpoint);
        socket.subscribe("*");
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted() && alive) {
            String recv;
            
            try {
                // UGLY HACK WARNING!
                // Unrecoverable exception from ZMQ, if we let recvStr
                // block and the thread gets terminated.
                String raw = socket.recvStr(ZMQ.DONTWAIT); 
                
                if (raw == null) {
                    Thread.sleep(5);
                    continue;
                } else {
                    recv = raw.trim();
                }
            } catch (Exception e) {
                break;
            }
            
            if (recv.isEmpty()) {
                continue;
            }
            
            String channel;
            String identifier;
            String message;
            
            if (recv.contains("\0")) {
                String[] split = recv.split("\0", 3);
            
                if (split.length != 3) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.WARNING, 
                                "Malformed packet received. Skipping.");
                    continue;
                }
            
                channel = split[0];
                identifier = split[1];
                message = split[2];
            } else {
                JSONParser parser = new JSONParser();
                Object obj;
                try {
                    obj = parser.parse(recv);
                } catch (ParseException ex) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.WARNING, 
                                "Malformed packet received. Skipping.");
                    continue;
                }
                JSONObject data = (JSONObject)obj;
                
                if (!data.containsKey("channel") || !data.containsKey("publisherid") || !data.containsKey("message")) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.WARNING, 
                                "Malformed packet received. JSON object missing channel, publisherid or message component. Skipping.");
                    continue;
                }
                
                channel = data.get("channel").toString();
                identifier = data.get("publisherid").toString();
                message = data.get("message").toString();
            }
            
            for (MessageCallback toRun : callbacks) {
                try {
                    // Let the callback figure out threading issues.
                    toRun.process(this.name, channel, identifier, message);
                } catch (Exception ex) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.SEVERE, 
                            "Error processing callback", ex);
                }
            }
        }
        
        cleanup();
    }
        
    public static void main (String[] args) throws InterruptedException, Exceptions.InvalidChannelException, Exceptions.InvalidNameException {
        NodePoint.DataStructureType = DataType.Json;
        
        ZContext context = new ZContext(1);
        ZAuth auth = new ZAuth(context, new ZCertStore.Hasher());
        auth.setVerbose(true);
        
        Tracking.setContext(context);
        Tracking.setAuthenticator(auth);
        
        NodePoint np = Tracking.getOrCreate(null, ZMQ.SUB, "subscriber");
        Subscriber sub = (Subscriber)np;
        
        Tracking.getAuthenticator().configureCurve("C:\\temp\\certs");
        sub.getSocket().setCurvePublicKey("*!!G^&m%iDVXWGB>V7g$j$>9%C%JmXTefszSnXyU".getBytes());
        sub.getSocket().setCurveSecretKey("Om]m<Khew8d*5[bML4R5St$Gkmt2LN*UM4TTXKjL".getBytes());
        sub.getSocket().setCurveServerKey("lN*r8I=:FuY@FZ&Mdn]HoOE!v@jx7kJ##Pf{S9>l".getBytes());
            
        sub.init(context);
        sub.connect("tcp://localhost:5556");
        sub.subscribe("*");
        
        sub.start();
        
        sub.addCallback(new MessageCallback() {
            public void process(String subscriber, String channel, String publisher, String message) {
                String msg = "Received %s from %s on channel %s";
                System.out.println(String.format(msg, message, publisher, channel));
            }
        });
        
        Thread.sleep(10000);
        
        sub.stop();
        auth.destroy();
        context.destroy();
    }
}