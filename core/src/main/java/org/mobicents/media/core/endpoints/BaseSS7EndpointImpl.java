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

package org.mobicents.media.core.endpoints;

import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.core.connections.AbstractConnection;
import org.mobicents.media.server.component.audio.AudioSplitter;
import org.mobicents.media.server.component.oob.OOBSplitter;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.ss7.SS7DataChannel;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 */
public class BaseSS7EndpointImpl extends AbstractEndpoint {

	protected AudioSplitter audioSplitter;
	protected OOBSplitter oobSplitter;

	private AtomicInteger loopbackCount = new AtomicInteger(0);
	private AtomicInteger readCount = new AtomicInteger(0);
	private AtomicInteger writeCount = new AtomicInteger(0);

	private SS7DataChannel ss7DataChannel;
	private int channelID;
	private boolean isALaw;

	private ChannelsManager channelsManager;

	public BaseSS7EndpointImpl(String localName, ChannelsManager channelsManager, int channelID, boolean isALaw) {
		super(localName);
		this.isALaw = isALaw;
		this.channelID = channelID;
		this.channelsManager = channelsManager;
	}

	@Override
	public void start() throws ResourceUnavailableException {
		super.start();

		audioSplitter = new AudioSplitter(getScheduler());
		oobSplitter = new OOBSplitter(getScheduler());
		audioSplitter.addOutsideComponent(mediaGroup.getAudioComponent());
		oobSplitter.addOutsideComponent(mediaGroup.getOOBComponent());

		try {
			ss7DataChannel = channelsManager.getSS7Channel(channelID, isALaw);
		} catch (Exception ex) {
			throw new ResourceUnavailableException("Can not open dahdi channel");
		}

		try {
			ss7DataChannel.setInputDsp(resourcesPool.getDspFactory().newProcessor());
			ss7DataChannel.setOutputDsp(resourcesPool.getDspFactory().newProcessor());
		} catch (Exception e) {
			// exception may happen only if invalid classes have been set in config
		}

		audioSplitter.addInsideComponent(ss7DataChannel.getAudioComponent());
		oobSplitter.addInsideComponent(ss7DataChannel.getOOBComponent());
	}

	@Override
	public void stop() {
		audioSplitter.releaseInsideComponent(ss7DataChannel.getAudioComponent());
		oobSplitter.releaseInsideComponent(ss7DataChannel.getOOBComponent());
		audioSplitter.releaseOutsideComponent(mediaGroup.getAudioComponent());
		oobSplitter.releaseOutsideComponent(mediaGroup.getOOBComponent());
		super.stop();
	}

	@Override
	public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
		Connection connection = super.createConnection(type, isLocal);
		audioSplitter.addOutsideComponent(((AbstractConnection) connection).getAudioComponent());
		oobSplitter.addOutsideComponent(((AbstractConnection) connection).getOOBComponent());

		if (getActiveConnectionsCount() == 1) {
			ss7DataChannel.bind();
			this.readCount.set(1);
			this.writeCount.set(1);
		}

		return connection;
	}

	@Override
	public void deleteConnection(Connection connection, ConnectionType connectionType) {
		super.deleteConnection(connection, connectionType);
		audioSplitter.releaseOutsideComponent(((AbstractConnection) connection).getAudioComponent());
		oobSplitter.releaseOutsideComponent(((AbstractConnection) connection).getOOBComponent());

		if (getActiveConnectionsCount() == 0) {
			ss7DataChannel.close();
			this.readCount.set(0);
			this.writeCount.set(0);
		}
	}

	@Override
	public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
		int readCount = 0, loopbackCount = 0, writeCount = 0;
		switch (oldMode) {
		case RECV_ONLY:
			readCount = -1;
			break;
		case SEND_ONLY:
			writeCount = -1;
			break;
		case SEND_RECV:
		case CONFERENCE:
			readCount = -1;
			writeCount = -1;
			break;
		case NETWORK_LOOPBACK:
			loopbackCount = -1;
			break;
		default:
			// XXX handle default case
			break;
		}

		switch (newMode) {
		case RECV_ONLY:
			readCount = +1;
			break;
		case SEND_ONLY:
			writeCount = +1;
			break;
		case SEND_RECV:
		case CONFERENCE:
			readCount = +1;
			writeCount = +1;
			break;
		case NETWORK_LOOPBACK:
			loopbackCount = +1;
			break;
		default:
			// XXX handle default case
			break;
		}

		if (readCount != 0 || writeCount != 0 || loopbackCount != 0) {
			// something changed
			loopbackCount = this.loopbackCount.addAndGet(loopbackCount);
			readCount = this.readCount.addAndGet(readCount);
			writeCount = this.writeCount.addAndGet(writeCount);

			if (loopbackCount > 0 || readCount == 0 || writeCount == 0) {
				audioSplitter.stop();
				oobSplitter.stop();
			} else {
				audioSplitter.start();
				oobSplitter.start();
			}
		}
	}

	public void setLoop(boolean toSet) {
		Boolean oldState = ss7DataChannel.inLoop();
		if (toSet && !oldState) {
			ss7DataChannel.activateLoop();
			loopbackCount.addAndGet(1);
			audioSplitter.stop();
			oobSplitter.stop();
		} else if (!toSet && oldState) {
			ss7DataChannel.deactivateLoop();
			loopbackCount.addAndGet(-1);
			if (loopbackCount.get() == 0 && readCount.get() > 0 && writeCount.get() > 0) {
				audioSplitter.start();
				oobSplitter.start();
			}
		}
	}

	@Override
	public void configure(boolean isALaw) {
		ss7DataChannel.setCodec(isALaw);
	}
}