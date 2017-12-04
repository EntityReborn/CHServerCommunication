package com.entityreborn.communication;

import com.entityreborn.communication.Exceptions.InvalidChannelException;
import com.entityreborn.communication.Exceptions.InvalidNameException;
import com.laytonsmith.extensions.chsc.Tracking;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.simple.JSONObject;
import org.zeromq.ZAuth;
import org.zeromq.ZCertStore;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
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
    
    public void init(ZContext context) {
        super.init(context, ZMQ.PUB);
        socket.setIdentity(publisherId.getBytes());
    }
    
    public void publish(String channel, String message) throws InvalidChannelException, InvalidNameException {
        publish(channel, message, publisherId);
    }
    
    public void publish(String channel, String message, String origpub) throws InvalidNameException, InvalidChannelException {
        String chan = channel.trim();
        
        if(!Util.isValidName(origpub)) {
            throw new InvalidNameException(origpub);
        }
        
        if(!Util.isValidChannel(channel)) {
            throw new InvalidChannelException(channel);
        }
        
        String tosend;
        
        if (NodePoint.DataStructureType == DataType.Json) {
            JSONObject obj = new JSONObject();
            
            obj.put("channel", chan);
            obj.put("publisherid", origpub);
            obj.put("message", message);
            
            tosend = obj.toJSONString();
        } else {
            tosend = chan + '\0' + origpub + '\0' + message;
        }
        
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
    
    public static void main(String[] args) throws InterruptedException, InvalidChannelException, InvalidNameException {
        NodePoint.DataStructureType = DataType.Json;
        
        ZContext context = new ZContext(1);
        ZAuth auth = new ZAuth(context, new ZCertStore.Hasher());
        auth.setVerbose(true);
        
        Tracking.setContext(context);
        Tracking.setAuthenticator(auth);
        
        NodePoint np = Tracking.getOrCreate(null, ZMQ.PUB, "publisher");
        Publisher pub = (Publisher)np;
        
        Tracking.getAuthenticator().configureCurve("C:\\temp\\certs");
        pub.getSocket().setCurveServer(true);
        pub.getSocket().setCurvePublicKey("lN*r8I=:FuY@FZ&Mdn]HoOE!v@jx7kJ##Pf{S9>l".getBytes());
        pub.getSocket().setCurveSecretKey("^ddCQsU-ofAh(QMhwrvKFzauJ+}H-yzhH[7AK?^C".getBytes());
            
        pub.init(context);
        pub.listen("tcp://*:5556");
        
        pub.start();
        
        for (int i=0; i < 100; i++) {
            System.out.println("Publishing " + i);
            pub.publish("SomeChannel", "data " + i);
            Thread.sleep(1000);
        }
        
        pub.stop();
        auth.destroy();
        context.destroy();
    }
}