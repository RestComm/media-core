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

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.utils.Text;

/**
 * Represents the connection activity.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpConnection implements ConnectionFailureListener {

	private static final AtomicInteger connectionID=new AtomicInteger(1);
    
	private final static Text REASON_CODE = new Text("902 Loss of lower layer connectivity");
	
	protected final Integer id;
    protected final Text textualId;
    protected MgcpEndpoint mgcpEndpoint;
    protected Connection connection;
    private SocketAddress callAgent;
    
    public MgcpConnection() {
        this.id = connectionID.getAndIncrement();
        this.textualId = new Text(Integer.toHexString(id));
    }
    
    public int getID() {
        return id;
    }

    public Text getTextualID() {
        return textualId;
    }
    
    public void setCallAgent(SocketAddress callAgent) {
    	this.callAgent=callAgent;
    }
    
    public void wrap(MgcpEndpoint mgcpEndpoint, Connection connection) {
    	this.mgcpEndpoint=mgcpEndpoint;
        this.connection = connection;
        this.connection.setConnectionFailureListener(this);        
    }
    
    public Connection getConnection() {
		return this.connection;
	}
   
    @Override
    public void onFailure() {
    	mgcpEndpoint.offer(this);
    	
    	MgcpEvent evt = (MgcpEvent) mgcpEndpoint.mgcpProvider.createEvent(MgcpEvent.REQUEST, callAgent);
		MgcpRequest msg = (MgcpRequest) evt.getMessage();        
		msg.setCommand(new Text("DLCX"));
		msg.setEndpoint(mgcpEndpoint.fullName);
		msg.setParameter(Parameter.CONNECTION_ID, textualId);
		msg.setTxID(MgcpEndpoint.txID.incrementAndGet());
		msg.setParameter(Parameter.REASON_CODE, MgcpConnection.REASON_CODE);
		mgcpEndpoint.send(evt, callAgent);		
    }
}
