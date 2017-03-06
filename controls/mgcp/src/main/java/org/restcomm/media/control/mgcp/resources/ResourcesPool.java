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

package org.restcomm.media.control.mgcp.resources;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.pooling.ResourcePool;
import org.restcomm.media.control.mgcp.connection.LocalConnectionImpl;
import org.restcomm.media.control.mgcp.connection.RtpConnectionImpl;
import org.restcomm.media.resources.dtmf.DetectorImpl;
import org.restcomm.media.resources.dtmf.GeneratorImpl;

/**
 * Implements connection's FSM.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class ResourcesPool implements ComponentFactory {
	
	private static final Logger logger = Logger.getLogger(ResourcesPool.class);

	// Media resources
	private final ResourcePool<AudioPlayerImpl> players;
	private final ResourcePool<AudioRecorderImpl> recorders;
	private final ResourcePool<DetectorImpl> dtmfDetectors;
	private final ResourcePool<GeneratorImpl> dtmfGenerators;

	// Connections
	private final ResourcePool<LocalConnectionImpl> localConnections;
	private final ResourcePool<RtpConnectionImpl> remoteConnections;

    public ResourcesPool(ResourcePool<RtpConnectionImpl> rtpConnections, ResourcePool<LocalConnectionImpl> localConnections,
            ResourcePool<AudioPlayerImpl> players, ResourcePool<AudioRecorderImpl> recorders,
            ResourcePool<DetectorImpl> dtmfDetectors, ResourcePool<GeneratorImpl> dtmfGenerators) {
        // Media Resources
        this.players = players;
        this.recorders = recorders;
        this.dtmfDetectors = dtmfDetectors;
        this.dtmfGenerators = dtmfGenerators;

        // Connections
        this.localConnections = localConnections;
        this.remoteConnections = rtpConnections;
    }

	@Override
	public Component newAudioComponent(ComponentType componentType) {
		Component result = null;
		
		switch (componentType) {
		case DTMF_DETECTOR:
			result = this.dtmfDetectors.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated DTMF Detector [pool size:" + dtmfDetectors.size() + ", free:" + dtmfDetectors.count()+"]");
			}
			break;

		case DTMF_GENERATOR:
		    result = this.dtmfGenerators.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated DTMF Generator [pool size:" + dtmfGenerators.size() + ", free:" + dtmfDetectors.count() + "]");
			}
			break;

		case PLAYER:
			result = this.players.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated Player [pool size:" + players.size() + ", free:" + players.count() +"]");
			}
			break;

		case RECORDER:
			result = this.recorders.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated Recorder [pool size:" + recorders.size() + ", free:" + recorders.count()+"]");
			}
			break;

		default:
			break;
		}

		return result;
	}

	@Override
	public void releaseAudioComponent(Component component, ComponentType componentType) {
		switch (componentType) {
		case DTMF_DETECTOR:
			this.dtmfDetectors.offer((DetectorImpl) component);
			if (logger.isDebugEnabled()) {
				logger.debug("Released DTMF Detector [pool size:" + dtmfDetectors.size() + ", free:" + dtmfDetectors.size() + "]");
			}
			break;

		case DTMF_GENERATOR:
			this.dtmfGenerators.offer((GeneratorImpl) component);

			if (logger.isDebugEnabled()) {
				logger.debug("Released DTMF Generator [pool size:" + dtmfGenerators.size() + ",free:" + dtmfGenerators.count() +"]");
			}
			break;

		case PLAYER:
			this.players.offer((AudioPlayerImpl) component);
			if (logger.isDebugEnabled()) {
				logger.debug("Released Player [pool size:" + players.size() + ", free:" + players.count()+"]");
			}
			break;

		case RECORDER:
			this.recorders.offer((AudioRecorderImpl) component);
			if (logger.isDebugEnabled()) {
				logger.debug("Released Recorder [pool size:" + recorders.size() + ", free:" + recorders.count()+"]");
			}
			break;

		default:
			break;
		}
	}

	@Override
	public Connection newConnection(boolean isLocal) {
		Connection result = null;
		if (isLocal) {
			result = this.localConnections.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated local connection [pool size:" + localConnections.size() + ", free:" + localConnections.count() +"]");
			}
		} else {
			result = this.remoteConnections.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated remote connection [pool size:" + remoteConnections.size() + ", free:" + remoteConnections.count()+"]");
			}
		}
		return result;
	}

	@Override
	public void releaseConnection(Connection connection, boolean isLocal) {
		if (isLocal) {
			this.localConnections.offer((LocalConnectionImpl) connection);
			if (logger.isDebugEnabled()) {
				logger.debug("Released local connection [pool size:" + localConnections.size() + ", free:" + localConnections.count() +"]");
			}
		} else {
			this.remoteConnections.offer((RtpConnectionImpl) connection);
			if (logger.isDebugEnabled()) {
				logger.debug("Released remote connection [pool size:" + remoteConnections.size() + ", free:" + remoteConnections.count()+"]");
			}
		}
	}
}