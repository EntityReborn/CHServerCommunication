/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.extensions.chsc;

import com.laytonsmith.PureUtilities.DaemonManager;
import com.laytonsmith.annotations.shutdown;
import com.laytonsmith.annotations.startup;
import com.laytonsmith.core.CHLog;
import com.laytonsmith.core.constructs.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.entityreborn.communication.Exceptions.InvalidNameException;
import com.entityreborn.communication.NodePoint;
import com.entityreborn.communication.Publisher;
import com.entityreborn.communication.Subscriber;
import com.entityreborn.communication.Subscriber.MessageCallback;
import com.entityreborn.communication.Util;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 *
 * @author import
 */
public class Tracking {
    
    @startup
    public static void startup() {
        System.out.println("CHServerCommunication starting...");
        context = ZMQ.context(1);
        System.out.println("CHServerCommunication started!");
    }
    
    @shutdown
    public static void shutdown() {
        System.out.println("CHServerCommunication shutting down...");
        Set<String> keys = publishers.keySet();
        for(String key : keys) { 
            Publisher pub = publishers.get(key);
            pub.stop();
        }
        
        publishers.clear();
        
        keys = subscribers.keySet();
        for (String key : keys) {
            Subscriber sub = subscribers.get(key);
            sub.stop();
        }
        
        subscribers.clear();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Tracking.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        context.term();
        context = null;
        
        System.out.println("CHServerCommunication shut down!");
    }
    
    private static final Map<String, Publisher> publishers = new HashMap<String, Publisher>();
    private static final Map<String, Subscriber> subscribers = new HashMap<String, Subscriber>();
    public static Context context;
    
    public static boolean hasPublisher(String name) throws InvalidNameException {
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        return publishers.containsKey(name);
    }
    
    public static boolean hasSubscriber(String name) throws InvalidNameException {
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        return subscribers.containsKey(name);
    }
    
    public static Publisher getPub(String name) throws InvalidNameException {
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        return publishers.get(name);
    }
    
    public static Subscriber getSub(String name) throws InvalidNameException {
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        return subscribers.get(name);
    }
    
    public static NodePoint getOrCreate(final DaemonManager daemon, int type, String name) throws InvalidNameException {
        NodePoint retn = null;
        
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        if (type == ZMQ.PUB) {
            retn = getPub(name);
            
            if (retn == null) {
                Publisher pub = new Publisher(name);
                pub.init(context);
                pub.start();
                
                publishers.put(name, pub);
                return pub;
            }
        } else if (type == ZMQ.SUB) {
            retn = getSub(name);
            
            if (retn == null) {
                Subscriber sub = new Subscriber(name);
                sub.init(context);
                sub.start();
                
                subscribers.put(name, sub);

                sub.addCallback(new MessageCallback() {
                    public void process(String subscriber, String channel, String publisher, String message) {
                        Events.fireReceived(daemon, subscriber, channel, publisher, message);
                    }
                });
                
                return sub;
            }
        }
        
        CHLog.GetLogger().i(CHLog.Tags.RUNTIME, name + " was not created!", Target.UNKNOWN);
        
        return retn;
    }

    public static boolean close(String name, int type) throws InvalidNameException {
        NodePoint node = null;
        
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        if (type == ZMQ.PUB) {
            node = publishers.remove(name);
        } else if (type == ZMQ.SUB) {
            node = subscribers.remove(name);
        }
        
        if (node != null) {
            node.stop();
            
            return true;
        }
        
        return false;
    }
}
