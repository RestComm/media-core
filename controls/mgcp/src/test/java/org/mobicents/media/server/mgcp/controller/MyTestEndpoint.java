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

import java.util.Collection;
import org.mobicents.media.ComponentType;
import org.mobicents.media.Component;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class MyTestEndpoint implements Endpoint {
    
    private String localName;
    
    public MyTestEndpoint(String localName) {
        this.localName = localName;
    }
    
    public String getLocalName() {
        return localName;
    }
    
    public int getActiveConnectionsCount()
    {
    	return 0;
    }

    public void setRtpConnections(int rtpConnections)
    {        	
    }
    
    public void setLocalConnections(int localConnections)
    {        	
    }
    
    public void setScheduler(Scheduler scheduler) {
    	throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public Scheduler getScheduler() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public EndpointState getState() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<MediaType> getMediaTypes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void start() throws ResourceUnavailableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Connection createConnection(ConnectionType type,Boolean isLocal) throws ResourceUnavailableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteConnection(Connection connection) {
        
    }

    public void deleteConnection(Connection connection,ConnectionType type) {
        
    }

    public void deleteAllConnections() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String describe(MediaType mediaType) throws ResourceUnavailableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setDspFactory(DspFactory dspFactory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void configure(boolean isALaw) {
    	throw new UnsupportedOperationException("Not supported yet.");
    }

    public void modeUpdated(ConnectionMode oldMode,ConnectionMode newMode) {    
    }
    
    public Component getResource(MediaType mediaType, ComponentType componentType) {
        return null;
    }
    
    public boolean hasResource(MediaType mediaType, ComponentType componentType) {
    	return false;
    }
    
    public void releaseResource(MediaType mediaType, ComponentType componentType) {    
    }
}
