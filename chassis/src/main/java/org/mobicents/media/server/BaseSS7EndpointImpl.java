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

package org.mobicents.media.server;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.io.ss7.SS7Manager;
import org.mobicents.media.server.connection.BaseConnection;
import org.mobicents.media.server.connection.Connections;
import org.mobicents.media.server.connection.LocalToRemoteConnections;
import org.mobicents.media.server.connection.RemoteToRemoteConnections;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Basic implementation of the endpoint.
 * 
 * @author kulikov
 * @author amit bhayani
 */
public abstract class BaseSS7EndpointImpl extends BaseEndpointImpl {
	protected SS7Manager ss7Manager; 
	
	protected int channelID;
	
	protected boolean isALaw;
	
	public BaseSS7EndpointImpl(String localName,int endpointType,SS7Manager ss7Manager,int channelID,boolean isALaw) {
        super(localName,endpointType);        
        this.ss7Manager=ss7Manager;
        this.channelID=channelID;
        this.isALaw=isALaw;
    }		
}
