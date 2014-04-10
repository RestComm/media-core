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

package org.mobicents.media.server.mgcp.controller;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.controller.signal.MgcpPackage;
import org.mobicents.media.server.mgcp.controller.signal.Signal;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.utils.Text;
/**
 *
 * @author kulikov
 */
public class Request {
    
    //request identifier provided by call agent.
    private Text ID = new Text(new byte[15], 0, 15);
    
    //notified entity parameter
    private NotifiedEntity callAgent = new NotifiedEntity();
    
    //executors grouped into packages
    private Collection<MgcpPackage> packages;
    
    //currently running signal
    private Signal currentSignal;
    
    //Endpoint executing this request
    private MgcpEndpoint endpoint;
    /**
     * The list of signals/events requested.
     * This list is cleared each time before accept new request.
     */ 
    private ConcurrentCyclicFIFO<Signal> executors = new ConcurrentCyclicFIFO<Signal>();
    
    //notified entity address
    private InetSocketAddress address;
    
    public Request(MgcpEndpoint endpoint, Collection<MgcpPackage> packages) {
        this.endpoint = endpoint;
        this.packages = packages;
        this.setRequest();
    }

    /**
     * Assigns this request executor to each package.
     */
    private void setRequest() {
        for (MgcpPackage p : packages) {
            p.setRequest(this);
        }
    }
    
    /**
     * Extracts signals and events from the given MGCP message and prepares
     * request for execution.
     * 
     * @param ID the identifier of the request provided by the call agent.
     * @param events the requested events parameter
     * @param signals the requested signals parameter
     */    
    public void accept(Text ID, Text callAgent, Collection<Text> events, Collection<Text> signals) 
            throws UnknownPackageException, UnknownEventException, UnknownSignalException {
        
    	//clean previously requested events and signals
    	executors.clear();
        
        //reset packages
        for (MgcpPackage p : packages) {
            p.reset();
        }
        
        //make a copy of the ID parameter
        ID.duplicate(this.ID);
        
        //make a copy of call agent parameter
        this.callAgent.setValue(callAgent);
        
        //queue events
        if (events != null) {
            Iterator<Text> it = events.iterator();
            while (it.hasNext()) {
                Text evt = it.next();
                queueEvent(evt);
            }
        }
        
        if (signals != null) {
            Iterator<Text> it = signals.iterator();
            //queue signals
            while (it.hasNext()) {
                queueSignal(it.next());
            }
        }
    }
    
    /**
     * Cancels execution of current request and cleans previously 
     * requested signals/events.
     * 
     */
    public void cancel() {
        if (currentSignal != null) {
            currentSignal.cancel();        
        }
    }
    
    /**
     * Starts the execution of 
     */
    public void execute() {
    	currentSignal = executors.poll();
    	if(currentSignal!=null)
    		currentSignal.execute();
    }
    
    public void onEvent(Text event) { 
    	address = new InetSocketAddress(callAgent.getHostName().toString(), callAgent.getPort());
        MgcpEvent evt = (MgcpEvent) endpoint.mgcpProvider.createEvent(MgcpEvent.REQUEST, address);
        MgcpRequest msg = (MgcpRequest) evt.getMessage();
        msg.setCommand(new Text("NTFY"));
        msg.setEndpoint(endpoint.fullName);
        msg.setParameter(Parameter.OBSERVED_EVENT, event);
        msg.setParameter(Parameter.NOTIFIED_ENTITY, callAgent.getValue());
        msg.setParameter(Parameter.REQUEST_ID, ID);
        msg.setTxID(MgcpEndpoint.txID.incrementAndGet());
        endpoint.send(evt, address);        
    }

    public void completed() {
        //start next signal
        execute();
    }

    public Endpoint getEndpoint() {
        return endpoint.getEndpoint();
    }

    public Connection getConnection(String ID) throws UnknownActivityException {
    	MgcpConnection mgcpConnection = this.endpoint.getConnection(Integer.valueOf(ID));
    	if(mgcpConnection == null) {
    		return null;
    	}
    	return mgcpConnection.getConnection();
    }
    
    private void queueEvent(Text event) throws UnknownPackageException, UnknownEventException {
        event.trim();
        
        Text pkgName = new Text();
        Text eventName = new Text();
        
        Text[] evt = new Text[] {pkgName, eventName};
        event.divide('/', evt);
        MgcpPackage p = this.getPackage(pkgName);
        if (p == null) {
        	throw new UnknownPackageException(eventName.toString());
        }
        p.accept(eventName);
    }
    
    private void queueSignal(Text event) throws UnknownPackageException, UnknownSignalException {
        Text pkgName = new Text();
        Text eventName = new Text();
        
        Text[] evt = new Text[] {pkgName, eventName};
        event.divide('/', evt);
        
        Text signalName = new Text();
        Text options = new Text();
        
        Text[] signal = new Text[]{signalName, options};
        
        eventName.divide(new char[]{'(', ')'}, signal);
        
        MgcpPackage p = this.getPackage(pkgName);
        Signal s = p.getSignal(signalName);
        
        if (s == null) {
            throw new UnknownSignalException(eventName.toString());
        }
        
        s.setTrigger(pkgName, eventName, options);
        executors.offer(s);
    }
    
    private MgcpPackage getPackage(Text name) throws UnknownPackageException {
        for (MgcpPackage p : packages) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        throw new UnknownPackageException(name.toString());
    }
}
