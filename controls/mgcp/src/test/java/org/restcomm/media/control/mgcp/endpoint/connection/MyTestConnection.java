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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.control.mgcp.endpoint.connection;

import java.io.IOException;

import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.control.mgcp.connection.BaseConnection;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.server.component.audio.AudioComponent;
import org.restcomm.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.spi.ConnectionFailureListener;

/**
 * @author yulian oifa
 */
public class MyTestConnection extends BaseConnection {

    private volatile boolean created;
    private volatile boolean opened;
    private volatile boolean closed;

    private AudioComponent audioComponent;
    private OOBComponent oobComponent;
    public MyTestConnection(int id,PriorityQueueScheduler scheduler) throws Exception {
        super(id, scheduler);
        audioComponent=new AudioComponent(-1);
        oobComponent=new OOBComponent(-1);
    }
    
    @Override
    public void generateCname() {
    	throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public String getCname() {
    	throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void generateOffer(boolean webrtc) throws IOException {
    	throw new UnsupportedOperationException("Not supported yet.");
    }

    public AudioComponent getAudioComponent()
    {
    	return audioComponent;
    }
    
    public OOBComponent getOOBComponent()
    {
    	return oobComponent;
    }
    
    public void setOtherParty(Connection other) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setOtherParty(byte[] descriptor) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getPacketsReceived(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getPacketsReceived() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public long getBytesReceived(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getBytesReceived() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getPacketsTransmitted(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getPacketsTransmitted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public long getBytesTransmitted(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getBytesTransmitted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getJitter(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getJitter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void setConnectionFailureListener(ConnectionFailureListener connectionListener)
    {
    	//currently used only in RTP Connection
    }

    @Override
    protected void onCreated() throws Exception {
        this.created = true;
    }

    public boolean isCreated() {
        return this.created;
    }

    @Override
    protected void onFailed() {
    }


    @Override
    protected void onOpened() throws Exception {
        this.opened = true;
    }

    public boolean isOpened() {
        return this.opened;
    }

    @Override
    protected void onClosed() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void setOtherParty(Text descriptor) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	public boolean isAvailable() {
		return true;
	}
}
