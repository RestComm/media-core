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

package org.mobicents.media.server.ctrl.mgcp.test;

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

/**
 *
 * @author kulikov
 */
public class Connection {
    private ConnectionIdentifier id;
    private ConnectionIdentifier secondConnId;
    private String sdp;
    private String remoteSDP;
    private EndpointIdentifier endpoint;
    private EndpointIdentifier secondEndpoint;

    public Connection(ConnectionIdentifier id) {
        this.id = id;
    }

    public EndpointIdentifier getEndpoint() {
        return endpoint;
    }

    public ConnectionIdentifier getId() {
        return id;
    }

    public String getLocalSdp() {
        return sdp;
    }

    public void setEndpoint(EndpointIdentifier endpoint) {
        this.endpoint = endpoint;
    }

    public void setLocalSdp(String sdp) {
        this.sdp = sdp;
    }

    public void setRemoteSDP(String remoteSDP) {
        this.remoteSDP = remoteSDP;
    }

    public String getRemoteSDP() {
        return remoteSDP;
    }

	public EndpointIdentifier getSecondEndpoint() {
		return secondEndpoint;
	}

	public void setSecondEndpoint(EndpointIdentifier secondEndpoint) {
		this.secondEndpoint = secondEndpoint;
	}

	public ConnectionIdentifier getSecondConnId() {
		return secondConnId;
	}

	public void setSecondConnId(ConnectionIdentifier secondConnId) {
		this.secondConnId = secondConnId;
	}
    
    
}
