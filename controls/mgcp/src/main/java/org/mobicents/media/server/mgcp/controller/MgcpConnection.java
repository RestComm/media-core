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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.media.server.concurrent.pooling.PoolResource;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.utils.Text;
/**
 * Represents the connection activity.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpConnection implements ConnectionFailureListener, PoolResource {
    
    private static final Logger LOGGER = Logger.getLogger(MgcpConnection.class);
    
    // Messages
    private static final Text REASON_CODE = new Text("902 Loss of lower layer connectivity");
    private static final Text DLCX = new Text("DLCX");
    
    // Unique ID Generator
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    
    // MGCP Connection Properties
	protected final int id;
    protected final Text hexadecimalId;
    private MgcpEndpoint mgcpEndpoint;
    
    // Runtime properties
    private MgcpCall call;
    private Connection connection;
    private SocketAddress callAgent;
    private Text descriptor = new Text();
    
    public MgcpConnection() {
        // MGCP Connection Properties
        this.id = ID_GENERATOR.getAndIncrement();
        this.hexadecimalId = new Text(Integer.toHexString(id));
    }
    
    public int getId() {
        return id;
    }

    public Text getHexadecimalId() {
        return hexadecimalId;
    }
    
    public int getCallId() {
        return (call == null) ? 0 : call.id;
	}
    
    protected MgcpEndpoint getEndpoint() {
        return this.mgcpEndpoint;
    }
    
    // TODO Restrict visibility to protect connection
    public Connection getConnection() {
        return this.connection;
    }
    
    public int getConnectionId() {
        return this.connection == null ? -1 : this.connection.getId();
    }
    
    /**
     * Assigns call object to which this connection belongs.
     * 
     * @param call the call object.
     */
    protected void setCall(MgcpCall call) {
        this.call = call;
        // TODO call should be responsible to do this!!
        call.connections.put(this.id,this);
    }
    
    public void setCallAgent(SocketAddress callAgent) {
    	this.callAgent=callAgent;
    }
    
    public void wrap(MgcpCall call, MgcpEndpoint endpoint, Connection connection) {
        this.call = call;
        this.mgcpEndpoint = endpoint;
        this.connection = connection;
        // TODO connection should be responsible to do this!!
        this.connection.setConnectionFailureListener(this);
        // TODO call should be responsible to do this!!
        call.connections.put(this.id, this);
    }
    
    public void setMode(Text mode) throws ModeNotSupportedException {
    	connection.setMode(ConnectionMode.valueOf(mode));
    }

    public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
        connection.setMode(mode);
    }
    
    public void setDtmfClamp(boolean dtmfClamp) {
        //connection.setDtmfClamp(dtmfClamp);
    }
    
    public Text getDescriptor() {
        String sdp = connection.getLocalDescriptor();
        descriptor.strain(sdp.getBytes(), 0, sdp.length());
        return descriptor;
    }
    
    /**
	 * Generates the local connection descriptor.
	 * 
	 * @throws IOException
	 */
    public void generateLocalDescriptor(boolean webrtc) throws IOException {
    	connection.generateOffer(webrtc);
    }

    public void setOtherParty(Text sdp) throws IOException {
        connection.setOtherParty(sdp);
    }
    
    public void setOtherParty(MgcpConnection other) throws IOException {
        this.connection.setOtherParty(other.connection);
    }
    
    /**
     * Terminates this activity and deletes connection.
     */
    public void release() {
        if (call != null) {
            // notify call about this activity termination
            call.exclude(this);
        }
    }
    
    public long getPacketsTransmitted() {
        return connection == null ? 0L : connection.getPacketsTransmitted();
    }
    
    public long getPacketsReceived() {
        return connection == null ? 0L : connection.getPacketsReceived();
    }

    @Override
    public void onFailure() {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("The MGCP connection " + this.id + "failed. Releasing connection now.");
        }
        
        // Send request to delete failed connection from its endpoint
        MgcpEvent evt = (MgcpEvent) mgcpEndpoint.mgcpProvider.createEvent(MgcpEvent.REQUEST, callAgent);
        MgcpRequest msg = (MgcpRequest) evt.getMessage();
        msg.setCommand(DLCX);
        msg.setEndpoint(mgcpEndpoint.fullName);
        msg.setParameter(Parameter.CONNECTION_ID, hexadecimalId);
        msg.setTxID(MgcpEndpoint.txID.incrementAndGet());
        msg.setParameter(Parameter.REASON_CODE, MgcpConnection.REASON_CODE);
        mgcpEndpoint.send(evt, callAgent);

        // Release the MGCP connection
        mgcpEndpoint.offer(this);
    }

    private void reset() {
        this.call = null;
        // XXX Cannot erase call agent, this value is needed in case a connection fails due to heartbeat failure
        // this.callAgent = null;
        this.connection = null;
        this.descriptor.strain(new byte[0], 0, 0);
    }

    /*
     * Pooled Resource API
     */
    @Override
    public void checkOut() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void checkIn() {
        reset();
    }
}
