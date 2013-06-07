/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.entityreborn.chservercommunication.ch;

import com.laytonsmith.abstraction.StaticLayer;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.events.AbstractEvent;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.exceptions.EventException;
import com.laytonsmith.core.exceptions.PrefilterNonMatchException;
import java.util.Map;

/**
 *
 * @author import
 */
public class Events {
    
    private static void fireEvent(final BindableEvent event, final String name) {
        StaticLayer.GetConvertor().runOnMainThreadLater(new Runnable() {
            public void run() {
                EventUtils.TriggerListener(Driver.EXTENSION, name, event);
            }
        });
    }

    public static void fireReceived(String channel, String id, String message) {
        RecvEvent event = new RecvEvent(channel, id, message);
        fireEvent(event, "srvcom_received");
    }
    
    private static class RecvEvent implements BindableEvent {

        private String channel;
        private String id;
        private String message;

        public RecvEvent(String channel, String id, String message) {
            this.channel = channel;
            this.id = id;
            this.message = message;
        }

        public Object _GetObject() {
            return this;
        }

        public String getChannel() {
            return channel;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    @api
    public static class srvcom_received extends AbstractEvent {
        public String getName() {
            return "comm_received";
        }

        public String docs() {
            return "";
        }

        public boolean matches(Map<String, Construct> prefilter, BindableEvent event)
                throws PrefilterNonMatchException {
            return true;
        }

        public BindableEvent convert(CArray manualObject) {
            return null;
        }

        public Map<String, Construct> evaluate(BindableEvent event)
                throws EventException {
            if (event instanceof RecvEvent) {
                RecvEvent e = (RecvEvent) event;
                
                Map<String, Construct> map = evaluate_helper(event);

                map.put("channel", new CString(e.getChannel(), Target.UNKNOWN));
                map.put("id", new CString(e.getId(), Target.UNKNOWN));
                map.put("message", new CString(e.getMessage(), Target.UNKNOWN));

                return map;
            } else {
                throw new EventException("Cannot convert e to RecvEvent");
            }
        }

        public Driver driver() {
            return Driver.EXTENSION;
        }

        public boolean modifyEvent(String key, Construct value,
                BindableEvent event) {
            return false;
        }

        public CHVersion since() {
            return CHVersion.V3_3_1;
        }
    }
}