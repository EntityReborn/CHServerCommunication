/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.chservercommunication.ch;

import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions;
import me.entityreborn.chservercommunication.NodePoint;
import me.entityreborn.chservercommunication.Publisher;
import me.entityreborn.chservercommunication.Subscriber;
import me.entityreborn.chservercommunication.Tracking;
import org.zeromq.ZMQ;

/**
 *
 * @author import
 */
public class Functions {
    public abstract static class CommFunc extends AbstractFunction {

        public Exceptions.ExceptionType[] thrown() {
            return null;
        }

        public boolean isRestricted() {
            return true;
        }

        public Boolean runAsync() {
            return false;
        }

        public CHVersion since() {
            return CHVersion.V3_3_1;
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class listen extends CommFunc {

        public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String stype = args[1].val().toUpperCase();
            String endpoint = args[2].val();
            
            int type = ZMQ.PUB;
            
            if (!stype.equals("PUB") && !stype.equals("SUB")) {
                throw new ConfigRuntimeException("You must specify PUB or SUB for comm_listen's second argument!", t);
            }
            
            if (stype.equals("SUB")) {
                type = ZMQ.SUB;
            }
            
            NodePoint node = Tracking.getOrCreate(type, name);
            
            node.listen(endpoint);
            
            return new CVoid(t);
        }

        public String getName() {
            return "comm_listen";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "void {name, type, endpoint} Listen. Type can be PUB or SUB.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class connect extends CommFunc {
        public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String stype = args[1].val().toUpperCase();
            String endpoint = args[2].val();
            
            int type = ZMQ.SUB;
            
            if (!stype.equals("PUB") && !stype.equals("SUB")) {
                throw new ConfigRuntimeException("You must specify PUB or SUB for comm_connect's second argument!", t);
            }
            
            if (stype.equals("PUB")) {
                type = ZMQ.PUB;
            }
            
            NodePoint node = Tracking.getOrCreate(type, name);
            
            node.connect(endpoint);
            
            return new CVoid(t);
        }

        public String getName() {
            return "comm_connect";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "void {name, type, endpoint} Connect. Type can be PUB or SUB.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class disconnect extends CommFunc {
        public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String stype = args[1].val().toUpperCase();
            String endpoint = args[2].val();
            
            int type = ZMQ.SUB;
            
            if (!stype.equals("PUB") && !stype.equals("SUB")) {
                throw new ConfigRuntimeException("You must specify PUB or SUB"
                        + " for comm_disconnect's second argument!", t);
            }
            
            if (stype.equals("PUB")) {
                type = ZMQ.PUB;
            }
            
            NodePoint node = Tracking.getSub(name);
            
            if (type == ZMQ.PUB) {
                node = Tracking.getPub(name);
            }
            
            if (node == null) {
                throw new ConfigRuntimeException("Unknown " + stype + " "
                        + " given to comm_disconnect!", t);
            }
            
            node.disconnect(endpoint);
            
            return new CVoid(t);
        }

        public String getName() {
            return "comm_disconnect";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "void {name, type, endpoint} Disconnect. Type can be PUB or SUB.";
        }
    }
    
    @api(environments = {CommandHelperEnvironment.class})
    public static class subscribe extends CommFunc {
        public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String channel = args[1].val();

            NodePoint node = Tracking.getSub(name);
            
            if (node == null) {
                throw new ConfigRuntimeException("Unknown SUB " + name + " given to comm_subscribe!", t);
            }
            
            ((Subscriber)node).subscribe(channel);
            
            return new CVoid(t);
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
    public static class unsubscribe extends CommFunc {
        public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String channel = args[1].val();

            NodePoint node = Tracking.getSub(name);
            
            if (node == null) {
                throw new ConfigRuntimeException("Unknown SUB " + name + " given to comm_unsubscribe!", t);
            }
            
            ((Subscriber)node).unsubscribe(channel);
            
            return new CVoid(t);
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
    public static class publish extends CommFunc {
        public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
            String name = args[0].val();
            String channel = args[1].val();
            String message = args[2].val();

            NodePoint node = Tracking.getPub(name);
            
            if (node == null) {
                throw new ConfigRuntimeException("Unknown PUB " + name + " given to comm_publish!", t);
            }
            
            ((Publisher)node).publish(channel, message);
            
            return new CVoid(t);
        }

        public String getName() {
            return "comm_publish";
        }

        public Integer[] numArgs() {
            return new Integer[]{3};
        }

        public String docs() {
            return "void {name, channel, message} Publish <message> to <channel>.";
        }
    }
}
