/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.mgcp.controller.signal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import org.mobicents.media.server.mgcp.controller.Request;
import org.mobicents.media.server.mgcp.controller.UnknownActivityException;
import org.mobicents.media.server.mgcp.controller.UnknownEventException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.utils.Text;

/**
 * MGCP event/signal package.
 * 
 * @author kulikov
 */
public class MgcpPackage {
    //The name of the package
    private Text name;
    
    //The list of executors
    private ArrayList<Signal> signals = new ArrayList();
    
    //The interface for upper level
    protected Request request;
    
    /**
     * Creates new package.
     * 
     * @param name the name of the package.
     * @param signals the list of executors for events/signals.     
     */
    public MgcpPackage(String name, Collection<Signal> signals) {
        this.name = new Text(name);
        this.setPackage(signals);
    }

    /**
     * Assigns this package to each signal
     */
    private void setPackage(Collection<Signal> signals) {
        for (Signal s : signals) {
            s.setPackage(this);
            this.signals.add(s);
        }
    }
    /**
     * Gets the name of this package.
     * 
     * @return the name of the package.
     */    
    public Text getName() {
        return name;
    }
    
    /**
     * Assign request executor.
     * 
     * @param request the request executor.
     */
    public void setRequest(Request request) {
        this.request = request;
    }
    
    public void accept(Text event) throws UnknownEventException {
        //this method tryies to assign this event to each signal in the package
        //signal decides accept this event or not and if event is not accepted
        //by any signal then exception throws indicating that this event is not supported
        boolean accepted = false;
        
/*        Text eventName = new Text();
        Text action = new Text();
        
        Text[] evt = new Text[]{eventName, action};
        event.divide(new char[]{'(', ')'}, evt);
        
        eventName.trim();
*/        
        for (Signal signal : signals) {
            accepted |= signal.accept(event);                        
        }
        if (!accepted) {
            throw new UnknownEventException(event.toString());
        }
    }
    
    public void reset() {
        for (Signal signal : signals) {
            signal.reset();
        }
    }
    
    /**
     * Loads specified signal.
     * 
     * @param eventName the fully qualified name of signal including package name.     * 
     * @return signal executor object.
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */     
    private Signal loadSignal(String eventName, Properties config) throws Exception {
        
        if (!config.containsKey(eventName)) {
            throw new IllegalArgumentException("Event/signal " + eventName + " is not implemented");
        }
        
        String className = config.getProperty(eventName);
        
        ClassLoader loader = this.getClass().getClassLoader();        
        Class def =loader.loadClass(className);
        
        //select constructor with string argument;
        Constructor constructor = null;
        Constructor[] constructors = def.getConstructors();
        
        for (Constructor cons : constructors) {
            Class[] paramTypes = cons.getParameterTypes();
            if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                constructor = cons;
                break;
            }
        }
        
        if (constructor == null) {
            throw new InstantiationException("Signal must have constructor with string name");
        }
        
        Signal s = (Signal)constructor.newInstance(eventName);        
        return s;
    }

    public void onEvent(Text event) {
    	request.onEvent(event);
    }

    public void completed() {
        request.completed();
    }

    public Endpoint getEndpoint() {
        return request.getEndpoint();
    }

    public Connection getConnection(String ID) throws UnknownActivityException {
        return request.getConnection(ID);
    }
    
    public Signal getSignal(Text name) {
        for (Signal s : signals) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        
        return null;
    }
    
}
