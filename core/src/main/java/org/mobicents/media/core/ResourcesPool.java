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

import org.apache.log4j.Logger;
import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.Component;
import org.mobicents.media.ComponentFactory;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.DspFactory;

import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;

import org.mobicents.media.server.impl.rtp.ChannelsManager;

import org.mobicents.media.core.connections.RtpConnectionImpl;
import org.mobicents.media.core.connections.LocalConnectionImpl;

import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGenerator;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
/**
 * Implements connection's FSM.
 *
 * @author Oifa Yulian
 */
public class ResourcesPool implements ComponentFactory {

	//local arrays
    //getters , setters
	
	private Scheduler scheduler;
	private ChannelsManager channelsManager;
	private DspFactory dspFactory;
	
	private ConcurrentCyclicFIFO<Component> players;
	private ConcurrentCyclicFIFO<Component> recorders;
	private ConcurrentCyclicFIFO<Component> dtmfDetectors;
	private ConcurrentCyclicFIFO<Component> dtmfGenerators;
	private ConcurrentCyclicFIFO<Component> signalDetectors;
	private ConcurrentCyclicFIFO<Component> signalGenerators;
	
	private int defaultPlayers;
	private int defaultRecorders;
	private int defaultDtmfDetectors;
	private int defaultDtmfGenerators;
	private int defaultSignalDetectors;
	private int defaultSignalGenerators;
	
	private int dtmfDetectorDbi=-35;
	
	private ConcurrentCyclicFIFO<Connection> localConnections;
	private ConcurrentCyclicFIFO<Connection> remoteConnections;
	
	private int defaultLocalConnections;
	private int defaultRemoteConnections;
	
	private AtomicInteger connectionId=new AtomicInteger(1);
	
	public  Logger logger = Logger.getLogger(ResourcesPool.class);
	
	private AtomicInteger localConnectionsCount=new AtomicInteger(0);
	private AtomicInteger rtpConnectionsCount=new AtomicInteger(0);
	
	private AtomicInteger playersCount=new AtomicInteger(0);
	private AtomicInteger recordersCount=new AtomicInteger(0);
	private AtomicInteger dtmfDetectorsCount=new AtomicInteger(0);
	private AtomicInteger dtmfGeneratorsCount=new AtomicInteger(0);
	private AtomicInteger signalDetectorsCount=new AtomicInteger(0);
	private AtomicInteger signalGeneratorsCount=new AtomicInteger(0);
	
	public ResourcesPool(Scheduler scheduler,ChannelsManager channelsManager,DspFactory dspFactory)
	{
		this.scheduler=scheduler;
		this.channelsManager=channelsManager;
		this.dspFactory=dspFactory;
		
		players=new ConcurrentCyclicFIFO();
		recorders=new ConcurrentCyclicFIFO();
		dtmfDetectors=new ConcurrentCyclicFIFO();
		dtmfGenerators=new ConcurrentCyclicFIFO();
		signalDetectors=new ConcurrentCyclicFIFO();
		signalGenerators=new ConcurrentCyclicFIFO();
		localConnections=new ConcurrentCyclicFIFO();
		remoteConnections=new ConcurrentCyclicFIFO();
	}
	
	public DspFactory getDspFactory()
	{
		return dspFactory;
	}
	
	public void setDefaultPlayers(int value)
	{
		this.defaultPlayers=value;
	}
	
	public void setDefaultRecorders(int value)
	{
		this.defaultRecorders=value;
	}
	
	public void setDefaultDtmfDetectors(int value)
	{
		this.defaultDtmfDetectors=value;
	}
	
	public void setDefaultDtmfGenerators(int value)
	{
		this.defaultDtmfGenerators=value;
	}
	
	public void setDefaultSignalDetectors(int value)
	{
		this.defaultSignalDetectors=value;
	}
	
	public void setDefaultSignalGenerators(int value)
	{
		this.defaultSignalGenerators=value;
	}
	
	public void setDefaultLocalConnections(int value)
	{
		this.defaultLocalConnections=value;
	}
	
	public void setDefaultRemoteConnections(int value)
	{
		this.defaultRemoteConnections=value;
	}
	
	public void setDtmfDetectorDbi(int value)
	{
		this.dtmfDetectorDbi=value;
	}
	
	public void start()
	{
		for(int i=0;i<defaultPlayers;i++)
		{
			AudioPlayerImpl player=new AudioPlayerImpl("player",scheduler);
			try {
				player.setDsp(dspFactory.newProcessor());
			}
			catch(Exception ex) {				
			}
			
			players.offer(player);
		}
		
		playersCount.set(defaultPlayers);
		
		for(int i=0;i<defaultRecorders;i++)
			recorders.offer(new AudioRecorderImpl(scheduler));
		
		recordersCount.set(defaultRecorders);
		
		for(int i=0;i<defaultDtmfDetectors;i++)
		{
			DetectorImpl detector=new DetectorImpl("detector",scheduler);
			detector.setVolume(dtmfDetectorDbi);	        
			dtmfDetectors.offer(detector);
		}
		
		dtmfDetectorsCount.set(defaultDtmfDetectors);
				
		for(int i=0;i<defaultDtmfGenerators;i++)
		{
			GeneratorImpl generator=new GeneratorImpl("generator",scheduler);
			dtmfGenerators.offer(generator);
			generator.setToneDuration(100);
	        generator.setVolume(-20);
		}
		
		dtmfGeneratorsCount.set(defaultDtmfGenerators);
		
		for(int i=0;i<defaultSignalDetectors;i++)
			signalDetectors.offer(new PhoneSignalDetector("signal detector",scheduler));

		signalDetectorsCount.set(defaultSignalDetectors);
		
		for(int i=0;i<defaultSignalGenerators;i++)
			signalGenerators.offer(new PhoneSignalGenerator("signal generator",scheduler));
		
		signalGeneratorsCount.set(defaultSignalGenerators);
		
		for(int i=0;i<defaultLocalConnections;i++)
			localConnections.offer(new LocalConnectionImpl(connectionId.incrementAndGet(),channelsManager));

		localConnectionsCount.set(defaultLocalConnections);
		
		for(int i=0;i<defaultRemoteConnections;i++)
			remoteConnections.offer(new RtpConnectionImpl(connectionId.incrementAndGet(),channelsManager,dspFactory));
		
		rtpConnectionsCount.set(defaultRemoteConnections);				
	}
	
	public Component newAudioComponent(ComponentType componentType)
	{
		Component result=null;
		switch(componentType)
		{
			case DTMF_DETECTOR:
				result=dtmfDetectors.poll();
				if(result==null)
				{
					result=new DetectorImpl("detector",scheduler);
					((DetectorImpl)result).setVolume(dtmfDetectorDbi);	        
					dtmfDetectorsCount.incrementAndGet();
				}
 
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new dtmf detector,pool size:" + dtmfDetectorsCount.get() + ",free:" + dtmfDetectors.size());				
				break;
			case DTMF_GENERATOR:
				result=dtmfGenerators.poll();
				if(result==null)
				{
					result=new GeneratorImpl("generator",scheduler);
					((GeneratorImpl)result).setToneDuration(80);
					((GeneratorImpl)result).setVolume(-20);
					dtmfGeneratorsCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new dtmf generator,pool size:" + dtmfGeneratorsCount.get() + ",free:" + dtmfDetectors.size());				
				break;
			case PLAYER:
				result=players.poll();
				if(result==null)
				{
					result=new AudioPlayerImpl("player",scheduler);
					try {
						((AudioPlayerImpl)result).setDsp(dspFactory.newProcessor());
					}
					catch(Exception ex) {
					}			
					
					playersCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new player,pool size:" + playersCount.get() + ",free:" + players.size());
				break;
			case RECORDER:
				result=recorders.poll();
				if(result==null)
				{
					result=new AudioRecorderImpl(scheduler);
					recordersCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new recorder,pool size:" + recordersCount.get() + ",free:" + recorders.size());				
				break;
			case SIGNAL_DETECTOR:
				result=signalDetectors.poll();
				if(result==null)
				{
					result=new PhoneSignalDetector("signal detector",scheduler);
					signalDetectorsCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new signal detector,pool size:" + signalDetectorsCount.get() + ",free:" + signalDetectors.size());				
				break;
			case SIGNAL_GENERATOR:
				result=signalGenerators.poll();
				if(result==null)
				{
					result=new PhoneSignalGenerator("signal generator",scheduler);
					signalGeneratorsCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new signal generator,pool size:" + signalGeneratorsCount.get() + ",free:" + signalGenerators.size());				
				break;
		}
		
		return result;
	}
    
	public void releaseAudioComponent(Component component,ComponentType componentType)
	{
		switch(componentType)
		{
			case DTMF_DETECTOR:
				dtmfDetectors.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released dtmf detector,pool size:" + dtmfDetectorsCount.get() + ",free:" + dtmfDetectors.size());				
				break;
			case DTMF_GENERATOR:
				dtmfGenerators.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released dtmf generator,pool size:" + dtmfGeneratorsCount.get() + ",free:" + dtmfGenerators.size());				
				break;
			case PLAYER:
				players.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released player,pool size:" + playersCount.get() + ",free:" + players.size());				
				break;
			case RECORDER:
				recorders.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released recorder,pool size:" + recordersCount.get() + ",free:" + recorders.size());				
				break;
			case SIGNAL_DETECTOR:
				signalDetectors.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released signal detector,pool size:" + signalDetectorsCount.get() + ",free:" + signalDetectors.size());				
				break;
			case SIGNAL_GENERATOR:
				signalGenerators.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released signal generator,pool size:" + signalGeneratorsCount.get() + ",free:" + signalGenerators.size());				
				break;
		}
	}
    
	public Connection newConnection(boolean isLocal)
	{
		Connection result=null;
		if(isLocal)
		{
			result=localConnections.poll();
			if(result==null)
			{
				result=new LocalConnectionImpl(connectionId.incrementAndGet(),channelsManager);
				localConnectionsCount.incrementAndGet();
			}
			
			if(logger.isDebugEnabled())				
				logger.debug("Allocated new local connection,pool size:" + localConnectionsCount.get() + ",free:" + localConnections.size());							
		}
		else
		{
			result=remoteConnections.poll();
			if(result==null)
			{
				result=new RtpConnectionImpl(connectionId.incrementAndGet(),channelsManager,dspFactory);
				rtpConnectionsCount.incrementAndGet();
			}
			
			if(logger.isDebugEnabled())				
				logger.debug("Allocated new rtp connection,pool size:" + rtpConnectionsCount.get() + ",free:" + remoteConnections.size());
		}
		
		return result;
	}
    
	public void releaseConnection(Connection connection,boolean isLocal)
	{
		if(isLocal)
		{
			localConnections.offer(connection);
			
			if(logger.isDebugEnabled())				
				logger.debug("Released local connection,pool size:" + localConnectionsCount.get() + ",free:" + localConnections.size());
		}
		else
		{
			remoteConnections.offer(connection);
			
			if(logger.isDebugEnabled())				
				logger.debug("Released rtp connection,pool size:" + rtpConnectionsCount.get() + ",free:" + remoteConnections.size());
		}
	}
}
