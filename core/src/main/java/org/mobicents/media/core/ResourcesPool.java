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

package org.mobicents.media.core;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.connections.LocalConnectionImpl;
import org.mobicents.media.core.connections.LocalConnectionPool;
import org.mobicents.media.core.connections.RtpConnectionImpl;
import org.mobicents.media.core.connections.RtpConnectionPool;
import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderPool;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerPool;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGenerator;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.pooling.ResourcePool;

/**
 * Implements connection's FSM.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class ResourcesPool implements ComponentFactory {
	
	private static final Logger logger = Logger.getLogger(ResourcesPool.class);

	// Core components
	private final PriorityQueueScheduler scheduler;
	private final DspFactory dspFactory;

	// Media resources
	private final ResourcePool<AudioPlayerImpl> players;
	private final ResourcePool<AudioRecorderImpl> recorders;
	private final ConcurrentCyclicFIFO<Component> dtmfDetectors;
	private final ConcurrentCyclicFIFO<Component> dtmfGenerators;
	private final ConcurrentCyclicFIFO<Component> signalDetectors;
	private final ConcurrentCyclicFIFO<Component> signalGenerators;

	private int defaultDtmfDetectors;
	private int defaultDtmfGenerators;
	private int defaultSignalDetectors;
	private int defaultSignalGenerators;

	private int dtmfDetectorDbi;
	
	private AtomicInteger dtmfDetectorsCount;
	private AtomicInteger dtmfGeneratorsCount;
	private AtomicInteger signalDetectorsCount;
	private AtomicInteger signalGeneratorsCount;

	// Connections
	private final ResourcePool<LocalConnectionImpl> localConnections;
	private final ResourcePool<RtpConnectionImpl> remoteConnections;

	public ResourcesPool(PriorityQueueScheduler scheduler, ChannelsManager channelsManager, DspFactory dspFactory, RtpConnectionPool rtpConnections, LocalConnectionPool localConnections, AudioPlayerPool players, AudioRecorderPool recorders) {
		this.scheduler = scheduler;
		this.dspFactory = dspFactory;

		this.players = players;
		this.recorders = recorders;
		this.dtmfDetectors = new ConcurrentCyclicFIFO<Component>();
		this.dtmfGenerators = new ConcurrentCyclicFIFO<Component>();
		this.signalDetectors = new ConcurrentCyclicFIFO<Component>();
		this.signalGenerators = new ConcurrentCyclicFIFO<Component>();
		this.localConnections = localConnections;
		this.remoteConnections = rtpConnections;
		
		this.dtmfDetectorDbi = -35;
		
		this.dtmfDetectorsCount = new AtomicInteger(0);
		this.dtmfGeneratorsCount = new AtomicInteger(0);
		this.signalDetectorsCount = new AtomicInteger(0);
		this.signalGeneratorsCount = new AtomicInteger(0);
	}

	public DspFactory getDspFactory() {
		return dspFactory;
	}

	public void setDefaultDtmfDetectors(int value) {
		this.defaultDtmfDetectors = value;
	}

	public void setDefaultDtmfGenerators(int value) {
		this.defaultDtmfGenerators = value;
	}

	public void setDefaultSignalDetectors(int value) {
		this.defaultSignalDetectors = value;
	}

	public void setDefaultSignalGenerators(int value) {
		this.defaultSignalGenerators = value;
	}

	public void setDtmfDetectorDbi(int value) {
		this.dtmfDetectorDbi = value;
	}

	public void start() {
		// Setup DTMF recognizers
		for (int i = 0; i < defaultDtmfDetectors; i++) {
			DetectorImpl detector = new DetectorImpl("detector", this.scheduler);
			detector.setVolume(this.dtmfDetectorDbi);
			this.dtmfDetectors.offer(detector);
		}
		this.dtmfDetectorsCount.set(this.defaultDtmfDetectors);

		// Setup DTMF generators
		for (int i = 0; i < this.defaultDtmfGenerators; i++) {
			GeneratorImpl generator = new GeneratorImpl("generator", this.scheduler);
			generator.setToneDuration(100);
			generator.setVolume(-20);
			this.dtmfGenerators.offer(generator);
		}
		this.dtmfGeneratorsCount.set(this.defaultDtmfGenerators);

		// Setup signal detectors
		for (int i = 0; i < this.defaultSignalDetectors; i++) {
			this.signalDetectors.offer(new PhoneSignalDetector("signal detector", this.scheduler));
		}
		this.signalDetectorsCount.set(this.defaultSignalDetectors);

		// Setup signal generators
		for (int i = 0; i < this.defaultSignalGenerators; i++) {
			this.signalGenerators.offer(new PhoneSignalGenerator("signal generator", this.scheduler));
		}
		this.signalGeneratorsCount.set(defaultSignalGenerators);
	}

	@Override
	public Component newAudioComponent(ComponentType componentType) {
		Component result = null;
		
		switch (componentType) {
		case DTMF_DETECTOR:
			result = this.dtmfDetectors.poll();
			if (result == null) {
				result = new DetectorImpl("detector", this.scheduler);
				((DetectorImpl) result).setVolume(this.dtmfDetectorDbi);
				this.dtmfDetectorsCount.incrementAndGet();
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Allocated new dtmf detector, pool size:" + dtmfDetectorsCount.get() + ", free:" + dtmfDetectors.size());
			}
			break;

		case DTMF_GENERATOR:
			result = this.dtmfGenerators.poll();
			if (result == null) {
				result = new GeneratorImpl("generator", this.scheduler);
				((GeneratorImpl) result).setToneDuration(80);
				((GeneratorImpl) result).setVolume(-20);
				this.dtmfGeneratorsCount.incrementAndGet();
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Allocated new dtmf generator, pool size:" + dtmfGeneratorsCount.get() + ", free:" + dtmfDetectors.size());
			}
			break;

		case PLAYER:
			result = this.players.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated new player [pool size:" + players.size() + ", free:" + players.count() +"]");
			}
			break;

		case RECORDER:
			result = this.recorders.poll();
			if (logger.isDebugEnabled()) {
				logger.debug("Allocated new recorder [pool size:" + recorders.size() + ", free:" + recorders.count()+"]");
			}
			break;

		case SIGNAL_DETECTOR:
			result = this.signalDetectors.poll();
			if (result == null) {
				result = new PhoneSignalDetector("signal detector", this.scheduler);
				this.signalDetectorsCount.incrementAndGet();
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Allocated new signal detector, pool size:" + signalDetectorsCount.get() + ", free:" + signalDetectors.size());
			}
			break;

		case SIGNAL_GENERATOR:
			result = this.signalGenerators.poll();
			if (result == null) {
				result = new PhoneSignalGenerator("signal generator", this.scheduler);
				this.signalGeneratorsCount.incrementAndGet();
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Allocated new signal generator, pool size:" + signalGeneratorsCount.get() + ", free:" + signalGenerators.size());
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
			this.dtmfDetectors.offer(component);

			if (logger.isDebugEnabled()) {
				logger.debug("Released dtmf detector,pool size:" + dtmfDetectorsCount.get() + ",free:" + dtmfDetectors.size());
			}
			break;

		case DTMF_GENERATOR:
			this.dtmfGenerators.offer(component);

			if (logger.isDebugEnabled()) {
				logger.debug("Released dtmf generator,pool size:" + dtmfGeneratorsCount.get() + ",free:" + dtmfGenerators.size());
			}
			break;

		case PLAYER:
			this.players.offer((AudioPlayerImpl) component);
			if (logger.isDebugEnabled()) {
				logger.debug("Released player [pool size:" + players.size() + ", free:" + players.count()+"]");
			}
			break;

		case RECORDER:
			this.recorders.offer((AudioRecorderImpl) component);
			if (logger.isDebugEnabled()) {
				logger.debug("Released recorder [pool size:" + recorders.size() + ", free:" + recorders.count()+"]");
			}
			break;

		case SIGNAL_DETECTOR:
			this.signalDetectors.offer(component);

			if (logger.isDebugEnabled()) {
				logger.debug("Released signal detector,pool size:" + signalDetectorsCount.get() + ",free:" + signalDetectors.size());
			}
			break;

		case SIGNAL_GENERATOR:
			this.signalGenerators.offer(component);

			if (logger.isDebugEnabled()) {
				logger.debug("Released signal generator,pool size:" + signalGeneratorsCount.get() + ",free:" + signalGenerators.size());
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
