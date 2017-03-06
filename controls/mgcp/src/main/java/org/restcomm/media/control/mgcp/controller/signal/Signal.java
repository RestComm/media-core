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

package org.restcomm.media.control.mgcp.controller.signal;

import java.util.ArrayList;

import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.control.mgcp.controller.UnknownActivityException;
/**
 * Bridges the gap between the MGCP signal/event model and server SPI.
 * 
 * The <code>Signal</code> can be 
 * 
 * @author kulikov
 */
public abstract class Signal {
    
    /** The name of the event */
    private Text name;
    
    /** Signal triggered this signal */
    private RequestedEvent trigger = new RequestedEvent();
    
    /** Requested events */
    protected ArrayList<Text> events = new ArrayList<Text>(15);
    
    /** Dispatches detected events and gets resources */
    private MgcpPackage mgcpPackage;
    
    public Signal(String name) {
        this.name = new Text(name);
    }
    
    /**
     * Gets the name of the signal.
     * 
     * @return the name of the signal.
     */
    public Text getName() {
        return name;
    }
    
    /**
     * Assigns package to which this signal belongs.
     * 
     * @param mgcpPackage package instance.
     */
    protected void setPackage(MgcpPackage mgcpPackage) {
        this.mgcpPackage = mgcpPackage;
    }
    
    /**
     * Gets access to the package to holding this signal.
     * 
     * @return package instance.
     */
    protected MgcpPackage getPackage() {
        return mgcpPackage;
    }
    
    /**
     * Triggers this signal executor.
     * 
     * @param packageName the name of package
     * @param event name the name of signal
     * @options parameters of the signal
     */
    public void setTrigger(Text packageName, Text eventName, Text options)  {
        trigger.setPackageName(packageName);
        trigger.setEventName(eventName);
        trigger.setParams(options);
    }
    
    /**
     * Gets the signal request which triggered this signal executor.
     * 
     * @return signal request;
     */
    protected RequestedEvent getTrigger() {
        return this.trigger;
    }
    
    /**
     * Executes this signal
     */
    public abstract void execute();
    
    public boolean accept(Text event) {
        events.add(event);
        return doAccept(event);
    }
    
    public abstract boolean doAccept(Text event);
    
    public void reset() {
        cancel();
        events.clear();
    }
    
    protected void sendEvent(Text evt) {
    	for (Text event : events) {
            if (event.equals(evt)) {
                mgcpPackage.onEvent(event);
            }
        }
    }
    
    protected void sendEvent(Text pckName, Text evtName, Text params) {    	
//        for (Text event : events) {
//            if (event.equals(evtName) && mgcpPackage != null) {
//                System.out.println("Checking event: " + event);
                //TODO: repace this ass
                String s = pckName.toString() + "/" + evtName.toString() + "(" + params + ")";
                mgcpPackage.onEvent(new Text(s));
//                break;
//            }
//        }
    }
    
    public abstract void cancel();
    
    protected void complete() {
        mgcpPackage.completed();
    }
    
    public Endpoint getEndpoint() {
        return mgcpPackage.getEndpoint();
    }

    public Connection getConnection(String ID) {
    	try {
			return this.mgcpPackage.getConnection(ID);
		} catch (UnknownActivityException e) {
			return null;
		}
    }
    
}
