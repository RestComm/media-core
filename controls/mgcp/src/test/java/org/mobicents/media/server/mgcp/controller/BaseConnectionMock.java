/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
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

import org.mobicents.media.core.connections.BaseConnection;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.utils.Text;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class BaseConnectionMock extends BaseConnection {

    public BaseConnectionMock(int id) {
        super(id, null);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ConnectionType getType() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void bind() throws IllegalStateException {
        // TODO Auto-generated method stub
    }

    @Override
    public void generateOffer(boolean webrtc) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setOtherParty(Connection other) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setOtherParty(byte[] descriptor) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setOtherParty(Text descriptor) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long getPacketsReceived() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getBytesReceived() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getPacketsTransmitted() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getBytesTransmitted() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getJitter() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isAvailable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void generateCname() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getCname() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AudioComponent getAudioComponent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OOBComponent getOOBComponent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void onClosed() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onFailed() {
        // TODO Auto-generated method stub
        
    }

}
