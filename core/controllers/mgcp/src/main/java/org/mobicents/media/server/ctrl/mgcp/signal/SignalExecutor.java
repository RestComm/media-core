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
import org.mobicents.media.server.ctrl.mgcp.UnknownActivityException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 * Executes signals of the single package.
 * 
 * @author kulikov
 */
public class SignalExecutor implements Dispatcher {
    //The name of the package
    private String name;
    private FastList<Signal> list = new FastList();
    
    private Dispatcher dispatcher;
    
    /**
     * Creates package with specified name;
     * 
     * @param name the name of the package to create.
     */
    public SignalExecutor(String name) {
        this.name = name;
    }
    
    /**
     * Gets the name of this package.
     * 
     * @return the name of the package.
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
     * Includes specified signal to the package.
     * 
     * @param signal signal to accept.
     */
    public void include(Signal signal) {
        signal.setDispatcher(this);
        list.add(signal);
    }
    
    /**
     * Excludes specified signal from the package.
     * 
     * @param signal signal to exclude.
     */
    public void exclude(Signal signal) {
        signal.setDispatcher(null);
        list.remove(signal);
    }
    
    public void accept(RequestedEvent event) throws UnknownEventException {
        //this method tryies to assign this event to each signal in the package
        //signal decides accept this event or not and if event is not accepted
        //by any signal then exception throws indicating that this event is not supported
        boolean accepted = false;
        
        for (Signal signal : list) {
            accepted |= signal.accept(event);
        }
        
        if (!accepted) {
            throw new UnknownEventException(event.toString());
        }
    }
    
    public void reset() {
        for (Signal signal : list) {
            signal.reset();
        }
    }
    
    /**
     * Gets the signal executor for the given event name.
     * 
     * @param eventName the event name
     * @return signal executor
     * @throws org.mobicents.media.server.ctrl.mgcp.signal.UnknownSignalException
     */
    public Signal getSignal(EventName eventName) throws UnknownSignalException, UnknownActivityException {
        //searching signal by name        
        for (Signal s : list) {
            if (Comparator.equals(s.getName(), eventName)) {
                s.trigger(eventName);
                return s;
            }
        }
        
        //signal nopt found
        throw new UnknownSignalException(eventName.toString());
    }

    public void onEvent(EventName event) {
        if (this.dispatcher != null) {
            dispatcher.onEvent(event);
        }
    }

    public void completed() {
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

    public Connection getConnection(String ID) throws UnknownActivityException {
        if (this.dispatcher != null) {
            return dispatcher.getConnection(ID);
        } 
        return null;
    }
}

