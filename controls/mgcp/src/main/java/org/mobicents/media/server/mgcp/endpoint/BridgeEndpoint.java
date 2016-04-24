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

package org.mobicents.media.server.mgcp.endpoint;

import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.spi.MediaType;

/**
 * 
 * A bridge end point allows two kinds of connections: - RTP (for remote RTP
 * media resources) and - Local (between the bridge end point and other MMS end
 * points).
 * 
 * The bridge end point mixes and forwards media between remote and local
 * connections. Media and events arriving to the bridge end point from remote
 * connections are mixed and forwarded to local connections. Respectively, media
 * and events arriving to the bridge end point from local connections are mixed
 * and forwarded to remote connections.
 * 
 * 
 * @author yulian oifa
 * @author Ivelin Ivanov
 */
public class BridgeEndpoint extends BaseSplitterEndpointImpl {

	public BridgeEndpoint(String localName) {
		super(localName);
	}

	@Override
	public Component getResource(MediaType mediaType, ComponentType componentType) {
		return null;
	}
}
