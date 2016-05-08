/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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
        
package org.mobicents.media.control.mgcp.connection;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRemoteConnection implements MgcpConnection {

    @Override
    public int getIdentifier() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getHexIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLocal() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public MgcpConnectionState getState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MgcpConnectionMode getMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMode(MgcpConnectionMode mode) throws IllegalStateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String halfOpen() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String open(String sdp) throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws IllegalStateException {
        // TODO Auto-generated method stub
        
    }

}
