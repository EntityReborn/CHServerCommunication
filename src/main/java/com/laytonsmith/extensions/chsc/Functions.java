/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.extensions.chsc;

import com.entityreborn.communication.Exceptions.InvalidChannelException;
import com.entityreborn.communication.Exceptions.InvalidNameException;
import com.entityreborn.communication.NodePoint;
import com.entityreborn.communication.Publisher;
import com.entityreborn.communication.Subscriber;
import com.laytonsmith.PureUtilities.DaemonManager;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.environments.StaticRuntimeEnv;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CREIOException;
import com.laytonsmith.core.exceptions.CRE.CREInsufficientArgumentsException;
import com.laytonsmith.core.exceptions.CRE.CRENotFoundException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.natives.interfaces.Mixed;
import org.zeromq.ZCert;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

/**
 *
 * @author import
 */
public class Functions {
    public abstract static class CommFunc extends AbstractFunction {
        public Class<? extends CREThrowable>[] thrown() {
            return new Class[]{CREFormatException.class, 
                CRENotFoundException.class, CREIOException.class};
        }
        
        public boolean isRestricted() {
            return true;
        }

        public Boolean runAsync() {
            return null; // Don't care.
        }

        public MSVersion since() {
            return MSVersion.V3_3_1;
        }
    }
    
    @api
    public static class comm_create extends CommFunc {
        public Mixed exec(Target t, Environment environment,
                          Mixed... args) throws ConfigRuntimeException {
            String stype = args[0].val();
            String name = args[1].val();
            int type = ZMQ.PUB;

            if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                throw new CRENotFoundException("You must specify PUB or SUB"
                        + " for comm_create's first argument!", t);
            }

            if ("SUB".equals(stype)) {
                type = ZMQ.SUB;
            }
            
            NodePoint node;
            DaemonManager daemon = environment.getEnv(StaticRuntimeEnv.class).GetDaemonManager();
            
            try {
                node = Tracking.getOrCreate(daemon, type, name);
            } catch (InvalidNameException ex) {
                throw new CREFormatException("Invalid name " + name + 
                        " given to comm_listen!", t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_create";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {type, name]} Create a node.";
        }
    }
    
    @api
    public static class comm_listen extends CommFunc {
        public Mixed exec(Target t, Environment environment, 
                Mixed... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String endpoint = args[1].val();
            int type = ZMQ.PUB;
            
            if (args.length == 3) {
                String stype = args[2].val().toUpperCase();

                if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                    throw new CRENotFoundException("You must specify PUB or SUB"
                            + " for comm_listen's third argument!", t);
                }

                if ("SUB".equals(stype)) {
                    type = ZMQ.SUB;
                }
            }
            
            NodePoint node;
            DaemonManager daemon = environment.getEnv(StaticRuntimeEnv.class).GetDaemonManager();
            
            try {
                node = Tracking.getOrCreate(daemon, type, name);
            } catch (InvalidNameException ex) {
                throw new CREFormatException("Invalid name " + name + 
                        " given to comm_listen!", t);
            }
            
            try {
                node.listen(endpoint);
                node.start();
            } catch (ZMQException e) {
                throw new CREIOException("Exception while listening: " + e.getMessage(), t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_listen";
        }

        public Integer[] numArgs() {
            return new Integer[]{2, 3};
        }

        public String docs() {
            return "void {name, endpoint[, type]} Listen. Automatically creates the"
                    + " the socket if it doesn't exist already."
                    + " Type can be PUB or SUB, but defaults to PUB.";
        }
    }
    
    @api
    public static class comm_connect extends CommFunc {
        public Mixed exec(Target t, Environment environment, 
                Mixed... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String endpoint = args[1].val();
            int type = ZMQ.SUB;
            
            if (args.length == 3) {
                String stype = args[2].val().toUpperCase();

                if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                    throw new CRENotFoundException("You must specify PUB or SUB"
                            + " for comm_connect's third argument!", t);
                }

                if ("PUB".equals(stype)) {
                    type = ZMQ.PUB;
                }
            }
            
            NodePoint node;
            DaemonManager daemon = environment.getEnv(StaticRuntimeEnv.class).GetDaemonManager();
            
            try {
                node = Tracking.getOrCreate(daemon, type, name);
            } catch (InvalidNameException ex) {
                throw new CREFormatException("Invalid name " + name + 
                        " given to comm_connect!", t);
            }
            
            try {
                node.connect(endpoint);
                node.start();
            } catch (ZMQException e) {
                throw new CREIOException("Exception while connecting: " + e.getMessage(), t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_connect";
        }

        public Integer[] numArgs() {
            return new Integer[]{2, 3};
        }

        public String docs() {
            return "void {name, endpoint[, type]} Connect. Automatically creates the"
                    + " the socket if it doesn't exist already. Type can be PUB "
                    + "or SUB, but defaults to SUB.";
        }
    }
    
    @api
    public static class comm_disconnect extends CommFunc {
        public Mixed exec(Target t, Environment environment, 
                Mixed... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String endpoint = args[1].val();
            NodePoint node;
            
            try {
                node = Tracking.getNode(name);
            } catch (InvalidNameException ex) {
                throw new CREFormatException("Invalid name " + name + 
                        " given to comm_disconnect!", t);
            }
            
            if (node == null) {
                throw new CRENotFoundException("Unknown " + name + " "
                        + " given to comm_disconnect!", t);
            }
            
            try {
                node.disconnect(endpoint);
            } catch (ZMQException e) {
                throw new CREIOException("Exception while disconnecting: " + e.getMessage(), t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_disconnect";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {name, endpoint} Disconnect.";
        }
    }
    
    @api
    public static class comm_close extends CommFunc {
        public Mixed exec(Target t, Environment environment, 
                Mixed... args) throws ConfigRuntimeException {
            String name = args[0].val();
            
            boolean found;
            
            try {
                found = Tracking.close(name);
            } catch (InvalidNameException ex) {
                throw new CREFormatException("Invalid name " + name + 
                        " given to comm_close!", t);
            } catch (ZMQException e) {
                throw new CREIOException("Exception while closing: " + e.getMessage(), t);
            }
            
            if (!found) {
                throw new CRENotFoundException("Unknown " + name + " "
                        + " given to comm_close!", t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_close";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "void {name} Close."
                    + " This will disconnect any and all connections and binds"
                    + " related to this name for this type.";
        }
    }

    @api
    public static class comm_publish extends CommFunc {
        public Mixed exec(Target t, Environment environment, 
                Mixed... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String channel = args[1].val();
            String message = args[2].val();
            String origpub = name;
            
            if (args.length == 4) {
                origpub = args[3].val();
            }

            NodePoint node;
            
            try {
                node = Tracking.getNode(name);
            
                if (node == null) {
                    throw new CRENotFoundException("Unknown PUB " + name + 
                            " given to comm_publish!", t);
                }
                
                if (!(node instanceof Publisher)) {
                    throw new CRENotFoundException(name + 
                            " given to comm_publish is not a publisher!", t);
                }
                
                ((Publisher)node).publish(channel, message, origpub);
            } catch (InvalidChannelException ex) {
                throw new CREFormatException("Invalid channel " + channel + 
                        " given to comm_publish!", t);
            } catch (InvalidNameException ex) {
                throw new CREFormatException("Invalid name " + name + 
                        " given to comm_publish!", t);
            } catch (ZMQException e) {
                throw new CREIOException("Exception while publishing: " + e.getMessage(), t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_publish";
        }

        public Integer[] numArgs() {
            return new Integer[]{3, 4};
        }

        public String docs() {
            return "void {name, channel, message[, originalid]} Publish <message>"
                    + " to <channel> of PUB with name <name>. if originalid is"
                    + " given, that publisher's name will be used instead of"
                    + " this one.";
        }
    }
    
    @api
    public static class comm_gencert extends CommFunc {
        public Mixed exec(Target t, Environment environment, 
                Mixed... args) throws ConfigRuntimeException {
            CArray arr = new CArray(t);
            ZCert cert = new ZCert();
            arr.set("public", cert.getPublicKeyAsZ85(), t);
            arr.set("secret", cert.getSecretKeyAsZ85(), t);
            return arr;
        }

        public String getName() {
            return "comm_gencert";
        }

        public Integer[] numArgs() {
            return new Integer[]{0};
        }

        public String docs() {
            return "array {} Return a certificate public/secret pair for use in encryption.";
        }
    }
    
    @api
    public static class comm_configsecurity extends CommFunc {
        public Mixed exec(Target t, Environment environment, 
                Mixed... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String publickey = args[1].val();
            String secretkey = args[2].val();
            String serverkey = null;
            CArray options = new CArray(t);
            
            /*
                options:
                    curve-server (boolean): whether this security configuration should be authorative for who connects.
                                            Really only makes sense for listeners. Defaults to true for Publishers.
                    zap-domain (string): what ZAP domain to listen to. Defaults to "globlal".
            
            */
            
            if (args.length == 4) {
                if (args[3] instanceof CString) {
                    serverkey = args[3].val();
                } else if (args[3] instanceof CArray) {
                    options = (CArray)args[3];
                }
            } else if (args.length == 5) {
                serverkey = args[3].val();
                options = (CArray)args[4];
            }

            NodePoint node;
            try {
                node = Tracking.getNode(name);
            } catch (InvalidNameException ex) {
                throw new CREFormatException("Invalid name " + name + 
                        " given to comm_configsecurity!", t);
            }

            if (node == null) {
                throw new CRENotFoundException("Unknown " + name + 
                    " given to comm_configsecurity!", t);
            }

            if (node instanceof Subscriber && serverkey == null) {
                throw new CREInsufficientArgumentsException("Serverkey wasn't specified for"
                        + " comm_configsecurity!", t);
            }

            if (node instanceof Publisher && serverkey != null) {
                System.out.println("Ignoring serverkey provided for comm_configsecurity. Not needed!");
            }
            
            Boolean curveServer = node instanceof Publisher;
            if (options.containsKey("curve-server") && options.get("curve-server", t) instanceof CBoolean) {
                curveServer = ((CBoolean)options.get("curve-server", t)).getBoolean();
            }
            
            node.getSocket().setCurveServer(curveServer);
            
            Tracking.configureCurve();
            
            String zapDomain = "global";
            if (options.containsKey("zap-domain") && options.get("zap-domain", t) instanceof CString) {
                zapDomain = (options.get("zap-domain", t).val());
            }
            
            node.getSocket().setZAPDomain(zapDomain);
            node.getSocket().setCurvePublicKey(publickey.getBytes());
            node.getSocket().setCurveSecretKey(secretkey.getBytes());
            
            if (serverkey != null) {
                node.getSocket().setCurveServerKey(serverkey.getBytes());
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_configsecurity";
        }

        public Integer[] numArgs() {
            return new Integer[]{3, 4, 5};
        }

        public String docs() {
            return "void {name, publickey, secretkey[, serverkey][, options]} Setup node named <name>"
                    + " for certificate based encryption and authorization. Use comm_gencert to generate values"
                    + " for publickey and secretkey. PUBs should have SUBs public cert file in their ./cert directory"
                    + " or authentication will fail. SUBs must specify have the PUBs publickey specified here for the"
                    + " serverkey argument. <serverkey> is required for SUBs, and is the public key of the server connecting to."
                    + " <options> can be an array, containing curve-server (boolean, defaults to false for SUBs and true"
                    + " for PUBs) and zap-domain (string, defaults to 'global'.)";
        }
    }
}
