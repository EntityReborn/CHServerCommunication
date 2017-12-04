/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.extensions.chsc;

import com.entityreborn.communication.Exceptions.InvalidNameException;
import com.entityreborn.communication.NodePoint;
import com.entityreborn.communication.Publisher;
import com.entityreborn.communication.Subscriber;
import com.entityreborn.communication.Subscriber.MessageCallback;
import com.entityreborn.communication.Util;
import com.laytonsmith.PureUtilities.DaemonManager;
import com.laytonsmith.PureUtilities.SimpleVersion;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.core.CHLog;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.extensions.AbstractExtension;
import com.laytonsmith.core.extensions.MSExtension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZAuth;
import org.zeromq.ZCertStore;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author import
 */
@MSExtension("CHServerCommunication")
public class Tracking extends AbstractExtension {
    private static File certDir;
    
    @Override
    public void onStartup() {
        System.out.println("CHServerCommunication starting up...");
        
        context = new ZContext(1);
        authentication = new ZAuth(context, new ZCertStore.Hasher());
        authentication.setVerbose(true);
        
        certDir = new File(getConfigDir(), "certs");
        if (!certDir.exists()) {
            certDir.mkdirs();
        }
        
        System.out.println("CHServerCommunication started!");
    }
    
    @Override
    public void onShutdown() {
        System.out.println("CHServerCommunication shutting down...");
        
        Set<String> keys = nodes.keySet();
        for (String key : keys) { 
            NodePoint pub = nodes.get(key);
            pub.stop();
        }
        
        nodes.clear();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Tracking.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (authentication != null) {
            authentication.destroy();
            authentication = null;
        }
        
        if (context != null) {
            context.destroy();
            context = null;
        }
        
        System.out.println("CHServerCommunication shut down!");
    }
    
    private static final Map<String, NodePoint> nodes = new HashMap<String, NodePoint>();
    public static ZContext context;
    public static ZAuth authentication;
    
    public static ZAuth getAuthenticator() {
        return authentication;
    }
    
    public static void setAuthenticator(ZAuth auth) {
        authentication = auth;
    }
    
    public static void configureCurve() {
        authentication.configureCurve(certDir.getAbsolutePath());
    }
    
    public static ZContext getContext() {
        return context;
    }
    
    public static void setContext(ZContext ctx) {
        context = ctx;
    }
    
    public static boolean hasPublisher(String name) throws InvalidNameException {
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        return nodes.containsKey(name);
    }
    
    public static boolean hasSubscriber(String name) throws InvalidNameException {
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        return nodes.containsKey(name) && nodes.get(name) instanceof Publisher;
    }
    
    public static NodePoint getNode(String name) throws InvalidNameException {
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        return nodes.get(name);
    }
    
    public static NodePoint getOrCreate(final DaemonManager daemon, int type, String name) throws InvalidNameException {
        NodePoint retn;
        
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        retn = getNode(name);
        
        if (retn != null) {
            return retn;
        }
        
        if (type == ZMQ.PUB) {
            Publisher pub = new Publisher(name);
            pub.init(context);

            nodes.put(name, pub);
            return pub;
        } else if (type == ZMQ.SUB) {
            Subscriber sub = new Subscriber(name);
            sub.init(context);

            nodes.put(name, sub);

            sub.addCallback(new MessageCallback() {
                public void process(String subscriber, String channel, String publisher, String message) {
                    Events.fireReceived(daemon, subscriber, channel, publisher, message);
                }
            });

            return sub;
        }
        
        CHLog.GetLogger().i(CHLog.Tags.RUNTIME, name + " was not created!", Target.UNKNOWN);
        
        return retn;
    }

    public static boolean close(String name) throws InvalidNameException {
        NodePoint node;
        
        if (!Util.isValidName(name)) {
            throw new InvalidNameException(name);
        }
        
        node = getNode(name);
        
        if (node != null) {
            nodes.remove(name);
            node.stop();
            
            return true;
        }
        
        return false;
    }

    public Version getVersion() {
        return new SimpleVersion(0,0,2);
    }
}
