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

package org.mobicents.media.server.connection;

import java.io.IOException;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.io.Pipe;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class LocalConnectionImpl extends BaseConnection {

    private LocalConnectionImpl otherConnection;
    private Pipe audioPipe = new PipeImpl();

    
    public LocalConnectionImpl(String id, Connections connections,Boolean isLocalToRemote) throws Exception {
        super(id, connections,isLocalToRemote);
    }
    
    
    @Override
    public void setOtherParty(Connection other) throws IOException {
        if (!(other instanceof LocalConnectionImpl)) {
            throw new IOException("Not compatible");
        }
        
        this.audioChannel.connect(((BaseConnection)other).audioChannel);

        try {
            join();
            ((BaseConnection)other).join();
        } catch (Exception e) {
        	throw new IOException(e);
        }
    }

    public void setOtherParty(Text descriptor) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setOtherParty(byte[] descriptor) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    @Override
    public long getPacketsReceived(MediaType media) {
        return this.audioChannel.splitter.getInput().getPacketsReceived();
    }

    @Override
    public long getBytesReceived(MediaType media) {
        return 0;
    }

    @Override
    public long getBytesReceived() {
        return 0;
    }
    
    @Override
    public long getPacketsTransmitted(MediaType media) {
        return this.audioChannel.mixer.getOutput().getPacketsTransmitted();
    }

    @Override
    public long getBytesTransmitted(MediaType media) {
        return 0;
    }

    @Override
    public long getBytesTransmitted() {
        return 0;
    }
    
    @Override
    public String toString() {
        return "Local Connection [" + getEndpoint().getLocalName() + "]";
    }

    
    @Override
    public double getJitter(MediaType media) {
        return 0;
    }

    @Override
    public double getJitter() {
        return 0;
    }

    @Override
    public void setConnectionFailureListener(ConnectionFailureListener connectionListener)
    {
    	//currently used only in RTP Connection
    }
    
    @Override
    protected void onCreated() throws Exception {
        descriptor = template.getSDP("127.0.0.1", "LOCAL", "ENP", getEndpoint().getLocalName(), 0, 0);
    }

    @Override
    protected void onFailed() {
    }

    @Override
    protected void onOpened() throws Exception {
    }

    @Override
    protected void onClosed() {
        try {
            setMode(ConnectionMode.INACTIVE);
        } catch (ModeNotSupportedException e) {
        }
        
        this.audioChannel.disconnect();
        //release connection
        connections.releaseConnection(this,ConnectionType.LOCAL);        
    }

}
