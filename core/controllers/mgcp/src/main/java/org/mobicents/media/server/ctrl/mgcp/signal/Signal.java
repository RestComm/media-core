/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.ctrl.mgcp.signal;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import javolution.util.FastList;
import org.apache.log4j.Logger;
import org.mobicents.media.server.ctrl.mgcp.UnknownActivityException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 * Defines the MGCP signal.
 * 
 * @author kulikov
 */
public abstract class Signal {
    
    /** The name of the event */
    private String name;
    
    /** Signal triggered this signal */
    private EventName trigger;
    
    /** Requested events */
    protected FastList<RequestedEvent> events = new FastList();
    
    /** Dispatches detected events and gets resources */
    private Dispatcher dispatcher;
    
    private Connection connection;
        
    public Signal(String name) {
        this.name = name;
    }
    
    /**
     * Gets the name of the signal.
     * 
     * @return the name of the signal.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Assigns callback handler for detected events.
     * 
     * @param dispatcher the handler.
     */
    public void setDispatcher(Dispatcher listener) {
        this.dispatcher = listener;
    }
    
    /**
     * Triggers this sgnal executor.
     * 
     * @param trigger the signal request cause trigger
     */
    protected void trigger(EventName trigger) throws UnknownActivityException {
        this.trigger = trigger;
        if (trigger.getConnectionIdentifier() != null) {
            connection = dispatcher.getConnection(trigger.getConnectionIdentifier().toString());
        }
    }
    
    /**
     * Gets the signal request wich triggered this signal executor.
     * 
     * @return signal request;
     */
    protected EventName getTrigger() {
        return this.trigger;
    }
    
    /**
     * Executes this signal
     */
    public abstract void execute();
    
    public boolean accept(RequestedEvent event) {
        events.add(event);
        return doAccept(event);
    }
    
    public abstract boolean doAccept(RequestedEvent event);
    
    public void reset() {
        cancel();
        events.clear();
    }
    
    protected void sendEvent(EventName evt) {
        for (RequestedEvent event : events) {
            if (Comparator.matches(event, evt) && dispatcher != null) {
                dispatcher.onEvent(evt);
            }
        }
    }
    
    public abstract void cancel();
    
    protected void complete() {
        if (this.dispatcher != null) {
            dispatcher.completed();
        }
    }
    
    public Endpoint getEndpoint() {
        if (this.dispatcher != null) {
            return dispatcher.getEndpoint();
        } 
        return null;
    }

    public Connection getConnection(String ID) {
//        if (this.dispatcher != null) {
//            return dispatcher.getConnection(ID);
//        } 
//        return null;
        return this.connection;
    }
    
}
