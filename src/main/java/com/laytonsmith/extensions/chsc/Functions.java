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
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.environments.GlobalEnv;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import org.zeromq.ZCert;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

/**
 *
 * @author import
 */
public class Functions {
    public abstract static class CommFunc extends AbstractFunction {
        public Exceptions.ExceptionType[] thrown() {
            return new ExceptionType[]{ExceptionType.FormatException, 
                ExceptionType.NotFoundException, ExceptionType.IOException};
        }
        
        public boolean isRestricted() {
            return true;
        }

        public Boolean runAsync() {
            return null; // Don't care.
        }

        public CHVersion since() {
            return CHVersion.V3_3_1;
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_create extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String stype = args[0].val();
            String name = args[1].val();
            int type = ZMQ.PUB;

            if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                throw new ConfigRuntimeException("You must specify PUB or SUB"
                        + " for comm_create's first argument!", Exceptions.ExceptionType.NotFoundException, t);
            }

            if ("SUB".equals(stype)) {
                type = ZMQ.SUB;
            }
            
            NodePoint node;
            DaemonManager daemon = environment.getEnv(GlobalEnv.class).GetDaemonManager();
            
            try {
                node = Tracking.getOrCreate(daemon, type, name);
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_listen!", Exceptions.ExceptionType.FormatException, t);
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
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_listen extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String endpoint = args[1].val();
            int type = ZMQ.PUB;
            
            if (args.length == 3) {
                String stype = args[2].val().toUpperCase();

                if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                    throw new ConfigRuntimeException("You must specify PUB or SUB"
                            + " for comm_listen's third argument!", Exceptions.ExceptionType.NotFoundException, t);
                }

                if ("SUB".equals(stype)) {
                    type = ZMQ.SUB;
                }
            }
            
            NodePoint node;
            DaemonManager daemon = environment.getEnv(GlobalEnv.class).GetDaemonManager();
            
            try {
                node = Tracking.getOrCreate(daemon, type, name);
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_listen!", Exceptions.ExceptionType.FormatException, t);
            }
            
            try {
                node.listen(endpoint);
                node.start();
            } catch (ZMQException e) {
                throw new ConfigRuntimeException("Exception while listening: " + e.getMessage(), 
                        Exceptions.ExceptionType.IOException, t);
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
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_connect extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String endpoint = args[1].val();
            int type = ZMQ.SUB;
            
            if (args.length == 3) {
                String stype = args[2].val().toUpperCase();

                if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                    throw new ConfigRuntimeException("You must specify PUB or SUB"
                            + " for comm_connect's third argument!", Exceptions.ExceptionType.NotFoundException, t);
                }

                if ("PUB".equals(stype)) {
                    type = ZMQ.PUB;
                }
            }
            
            NodePoint node;
            DaemonManager daemon = environment.getEnv(GlobalEnv.class).GetDaemonManager();
            
            try {
                node = Tracking.getOrCreate(daemon, type, name);
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_connect!", Exceptions.ExceptionType.FormatException, t);
            }
            
            try {
                node.connect(endpoint);
                node.start();
            } catch (ZMQException e) {
                throw new ConfigRuntimeException("Exception while connecting: " + e.getMessage(), 
                        Exceptions.ExceptionType.IOException, t);
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
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_disconnect extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String endpoint = args[1].val();
            int type = ZMQ.SUB;
            
            if (args.length == 3) {
                String stype = args[2].val().toUpperCase();

                if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                    throw new ConfigRuntimeException("You must specify PUB or SUB"
                            + " for comm_disconnect's third argument!", Exceptions.ExceptionType.NotFoundException, t);
                }

                if ("PUB".equals(stype)) {
                    type = ZMQ.PUB;
                }
            }
            
            NodePoint node;
            
            try {
                if (type == ZMQ.PUB) {
                    node = Tracking.getPub(name);
                } else {
                    node = Tracking.getSub(name);
                }
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_disconnect!", Exceptions.ExceptionType.FormatException, t);
            }
            
            if (node == null) {
                throw new ConfigRuntimeException("Unknown " + name + " "
                        + " given to comm_disconnect!", Exceptions.ExceptionType.NotFoundException, t);
            }
            
            try {
                node.disconnect(endpoint);
            } catch (ZMQException e) {
                throw new ConfigRuntimeException("Exception while disconnecting: " + e.getMessage(), 
                        Exceptions.ExceptionType.IOException, t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_disconnect";
        }

        public Integer[] numArgs() {
            return new Integer[]{2, 3};
        }

        public String docs() {
            return "void {name, endpoint[, type]} Disconnect. Type can be PUB or"
                    + " SUB, but defaults to SUB.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_close extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            int type = ZMQ.SUB;
            
            if (args.length == 2) {
                String stype = args[1].val().toUpperCase();

                if (!"PUB".equals(stype) && !"SUB".equals(stype)) {
                    throw new ConfigRuntimeException("You must specify PUB or SUB"
                            + " for comm_close's second argument!", Exceptions.ExceptionType.NotFoundException, t);
                }

                if ("PUB".equals(stype)) {
                    type = ZMQ.PUB;
                }
            }
            
            boolean found;
            
            try {
                found = Tracking.close(name, type);
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_close!", Exceptions.ExceptionType.FormatException, t);
            } catch (ZMQException e) {
                throw new ConfigRuntimeException("Exception while closing: " + e.getMessage(), 
                        Exceptions.ExceptionType.IOException, t);
            }
            
            if (!found) {
                throw new ConfigRuntimeException("Unknown " + name + " "
                        + " given to comm_close!", Exceptions.ExceptionType.NotFoundException, t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_close";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {name, type} Close. Type can be PUB or SUB."
                    + " This will disconnect any and all connections and binds"
                    + " related to this name for this type.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_setdatatype extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            if (args.length == 1) {
                String stype = args[0].val().toUpperCase();

                if (!"JSON".equals(stype) && !"NULLSEPARATED".equals(stype)) {
                    throw new ConfigRuntimeException("You must specify JSON or NULLSEPARATED"
                            + " for comm_setdatatype's first argument!", Exceptions.ExceptionType.NotFoundException, t);
                }

                if ("JSON".equals(stype)) {
                    NodePoint.DataStructureType = NodePoint.DataType.Json;
                } else {
                    NodePoint.DataStructureType = NodePoint.DataType.NullSeparated;
                }
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_setdatatype";
        }

        public Integer[] numArgs() {
            return new Integer[]{1};
        }

        public String docs() {
            return "void {type} Type can be JSON or NULLSEPARATED."
                    + " This changes the internal data type used to send messages.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_getdatatype extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            return CString.GetConstruct(NodePoint.DataStructureType.name().toUpperCase());
        }

        public String getName() {
            return "comm_getdatatype";
        }

        public Integer[] numArgs() {
            return new Integer[]{0};
        }

        public String docs() {
            return "string {} Type will be JSON or NULLSEPARATED."
                    + " This returns the internal data type used to send messages.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_subscribe extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String channel = args[1].val();

            NodePoint node;
            
            try {
                node = Tracking.getSub(name);
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_subscribe!", Exceptions.ExceptionType.FormatException, t);
            }
            
            if (node == null) {
                throw new ConfigRuntimeException("Unknown SUB " + name + 
                        " given to comm_subscribe!", Exceptions.ExceptionType.NotFoundException, t);
            }
            
            try {
                ((Subscriber)node).subscribe(channel);
            } catch (InvalidChannelException ex) {
                throw new ConfigRuntimeException("Invalid channel " + channel + 
                        " given to comm_subscribe!", Exceptions.ExceptionType.FormatException, t);
            } catch (ZMQException e) {
                throw new ConfigRuntimeException("Exception while subscribing: " + e.getMessage(), 
                        Exceptions.ExceptionType.IOException, t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_subscribe";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {name, channel} Subscribe SUB <name> to <channel>.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_unsubscribe extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String channel = args[1].val();

            NodePoint node;
            
            try {
                node = Tracking.getSub(name);
            } catch (com.entityreborn.communication.Exceptions.InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_unsubscribe!", Exceptions.ExceptionType.FormatException, t);
            }
            
            if (node == null) {
                throw new ConfigRuntimeException("Unknown SUB " + name + 
                        " given to comm_unsubscribe!", Exceptions.ExceptionType.NotFoundException, t);
            }
            
            try {
                ((Subscriber)node).unsubscribe(channel);
            } catch (InvalidChannelException ex) {
                throw new ConfigRuntimeException("Invalid channel " + channel + 
                        " given to comm_subscribe!", Exceptions.ExceptionType.FormatException, t);
            } catch (ZMQException e) {
                throw new ConfigRuntimeException("Exception while unsubscribing: " + e.getMessage(), 
                        Exceptions.ExceptionType.IOException, t);
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_unsubscribe";
        }

        public Integer[] numArgs() {
            return new Integer[]{2};
        }

        public String docs() {
            return "void {name, channel} Unsubscribe SUB <name> from <channel>.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_publish extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String channel = args[1].val();
            String message = args[2].val();
            String origpub = name;
            
            if (args.length == 4) {
                origpub = args[3].val();
            }

            NodePoint node;
            
            try {
                node = Tracking.getPub(name);
            
                if (node == null) {
                    throw new ConfigRuntimeException("Unknown PUB " + name + 
                            " given to comm_publish!", Exceptions.ExceptionType.NotFoundException, t);
                }
                
                ((Publisher)node).publish(channel, message, origpub);
            } catch (InvalidChannelException ex) {
                throw new ConfigRuntimeException("Invalid channel " + channel + 
                        " given to comm_publish!", Exceptions.ExceptionType.FormatException, t);
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Invalid name " + name + 
                        " given to comm_publish!", Exceptions.ExceptionType.FormatException, t);
            } catch (ZMQException e) {
                throw new ConfigRuntimeException("Exception while publishing: " + e.getMessage(), 
                        Exceptions.ExceptionType.IOException, t);
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
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_gencert extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
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
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class comm_configsecurity extends CommFunc {
        public Construct exec(Target t, Environment environment, 
                Construct... args) throws ConfigRuntimeException {
            String type = args[0].val();
            String name = args[1].val();
            String publickey = args[2].val();
            String secretkey = args[3].val();
            String serverkey = null;
            
            if (args.length == 5) {
                serverkey = args[4].val();
            }

            NodePoint node;
            try {
                if ("SUB".equalsIgnoreCase(type)) {
                    node = Tracking.getSub(name);
                    
                    if (node == null) {
                        throw new ConfigRuntimeException("Unknown SUB " + name + 
                            " given to comm_configsecurity!", Exceptions.ExceptionType.NotFoundException, t);
                    }
                    
                    if (serverkey == null) {
                        throw new ConfigRuntimeException("Serverkey wasn't specified for"
                                + " comm_configuresecurity!", Exceptions.ExceptionType.InsufficientArgumentsException, t);
                    }
                } else if ("PUB".equalsIgnoreCase(type)){
                    node = Tracking.getPub(name);
                    
                    if (node == null) {
                        throw new ConfigRuntimeException("Unknown PUB " + name + 
                            " given to comm_configsecurity!", Exceptions.ExceptionType.NotFoundException, t);
                    }
                    
                    if (serverkey != null) {
                        System.out.println("Ignoring serverkey provided for comm_configuresecurity. Not needed!");
                    }
                    
                    node.getSocket().setCurveServer(true);
                } else {
                    throw new ConfigRuntimeException("Unknown type: " + type + 
                            " given to comm_configuresecurity!", Exceptions.ExceptionType.NotFoundException, t);
                }
            } catch (InvalidNameException ex) {
                throw new ConfigRuntimeException("Unknown " + type + " " + name + 
                            " given to comm_configuresecurity!", Exceptions.ExceptionType.NotFoundException, t);
            }
            
            Tracking.configureCurve();
            
            node.getSocket().setZAPDomain("global");
            node.getSocket().setCurvePublicKey(publickey.getBytes());
            node.getSocket().setCurveSecretKey(secretkey.getBytes());
            
            if ("SUB".equalsIgnoreCase(type) && serverkey != null) {
                node.getSocket().setCurveServerKey(serverkey.getBytes());
            }
            
            return CNull.NULL;
        }

        public String getName() {
            return "comm_configsecurity";
        }

        public Integer[] numArgs() {
            return new Integer[]{4, 5};
        }

        public String docs() {
            return "void {type, name, publickey, secretkey[, serverkey]} Setup <type> named <name>"
                    + " for certificate based encryption and authorization. Use comm_gencert to generate values"
                    + " for publickey and secretkey. PUBs should have SUBs public cert file in their ./cert directory"
                    + " or authentication will fail. SUBs must specify have the PUBs publickey specified here for the"
                    + " serverkey argument.";
        }
    }
}
