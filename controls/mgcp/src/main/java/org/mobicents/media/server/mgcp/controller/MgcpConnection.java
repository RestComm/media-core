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
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.utils.Text;

/**
 * Represents the connection activity.
 * 
 * @author kulikov
 */
public class MgcpConnection {
    protected Text id;
    protected MgcpCall call;
    protected MgcpEndpoint mgcpEndpoint;
    protected Connection connection;
    
    private Text descriptor = new Text();
    
    public MgcpConnection() {
        id = new Text(Long.toHexString(System.nanoTime()));
    }
    
    public Text getID() {
        return id;
    }

    /**
     * Assigns call object to which this connection belongs.
     * 
     * @param call the call object.
     */
    protected void setCall(MgcpCall call) {
        this.call = call;
        call.connections.add(this);
    }
    
    public void wrap(MgcpEndpoint mgcpEndpoint, MgcpCall call, Connection connection) {
        this.call = call;
        this.connection = connection;
        synchronized(call.connections) {
            call.connections.add(this);
        }
    }
    
    public void setMode(Text mode) throws ModeNotSupportedException {
    	connection.setMode(ConnectionMode.valueOf(mode));
    }

    public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
        connection.setMode(mode);
    }
    
    public void setGain(int gain) {
        connection.setGain(gain);
    }
    
    public void setDtmfClamp(boolean dtmfClamp) {
        connection.setDtmfClamp(dtmfClamp);
    }
    
    public Text getDescriptor() {
        descriptor.strain(connection.getDescriptor().getBytes(), 0, connection.getDescriptor().length());
        return descriptor;
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
        //notify call about this activity termination
        call.exclude(this);
    }
    
    public int getPacketsTransmitted() {
        return (int) connection.getPacketsTransmitted(MediaType.AUDIO);
    }
    
    public int getPacketsReceived() {
        return (int) connection.getPacketsReceived(MediaType.AUDIO);
    }
}
