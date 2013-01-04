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

import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;

import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.core.ResourcesPool;

import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGenerator;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;

/**
 * Implements Local Components Holder for endpoint
 * Usefull for jsr 309 structure
 * @author Oifa Yulian
 */

public class MediaGroup {

	private Component player;
	private Component recorder;
	private Component dtmfDetector;
	private Component dtmfGenerator;
	private Component signalDetector;
	private Component signalGenerator;
	
	private ResourcesPool resourcesPool;
	
	private AudioComponent audioComponent;
	private OOBComponent oobComponent;
	
	private Endpoint endpoint;
	
	private int readComponents=0,writeComponents=0;
	private int readDtmfComponents=0,writeDtmfComponents=0;
	
	public MediaGroup(ResourcesPool resourcesPool,Endpoint endpoint)
	{
		this.resourcesPool=resourcesPool;
		this.audioComponent=new AudioComponent(0);
		this.oobComponent=new OOBComponent(0);
		this.endpoint=endpoint;
	} 
	
	public AudioComponent getAudioComponent()
	{
		return this.audioComponent;
	}
	
	public OOBComponent getOOBComponent()
	{
		return this.oobComponent;
	}
	
	public Component getPlayer()
	{
		if(this.player==null)
		{
			this.player=resourcesPool.newAudioComponent(ComponentType.PLAYER);
			this.player.setEndpoint(endpoint);
			audioComponent.addInput(((AudioPlayerImpl)this.player).getAudioInput());
			readComponents++;
			audioComponent.updateMode(true,writeComponents!=0);
			updateEndpoint(1,0);
		}
		return this.player;
	}
	
	public void releasePlayer()
	{
		if(this.player!=null)
		{
			audioComponent.remove(((AudioPlayerImpl)this.player).getAudioInput());
			readComponents--;
			audioComponent.updateMode(readComponents!=0,writeComponents!=0);			
			updateEndpoint(-1,0);
			this.player.clearEndpoint();
			((AudioPlayerImpl)this.player).clearAllListeners();
			this.player.deactivate();			
			resourcesPool.releaseAudioComponent(this.player,ComponentType.PLAYER);
			this.player=null;
		}
	}
	
	public boolean hasPlayer()
	{
		return this.player!=null;
	}
	
	public Component getRecorder()
	{
		if(this.recorder==null)
		{
			this.recorder=resourcesPool.newAudioComponent(ComponentType.RECORDER);
			this.recorder.setEndpoint(endpoint);
			audioComponent.addOutput(((AudioRecorderImpl)this.recorder).getAudioOutput());
			oobComponent.addOutput(((AudioRecorderImpl)this.recorder).getOOBOutput());
			writeComponents++;
			writeDtmfComponents++;
			audioComponent.updateMode(readComponents!=0,true);
			oobComponent.updateMode(readDtmfComponents!=0,true);
			updateEndpoint(0,1);
		}
		
		return this.recorder;
	}
	
	public void releaseRecorder()
	{
		if(this.recorder!=null)
		{
			audioComponent.remove(((AudioRecorderImpl)this.recorder).getAudioOutput());
			oobComponent.remove(((AudioRecorderImpl)this.recorder).getOOBOutput());
			writeComponents--;
			writeDtmfComponents--;
			audioComponent.updateMode(readComponents!=0,writeComponents!=0);			
			oobComponent.updateMode(readDtmfComponents!=0,writeDtmfComponents!=0);
			updateEndpoint(0,-1);
			this.recorder.clearEndpoint();
			((AudioRecorderImpl)this.recorder).clearAllListeners();
			this.recorder.deactivate();
			resourcesPool.releaseAudioComponent(this.recorder,ComponentType.RECORDER);
			this.recorder=null;
		}
	}
	
	public boolean hasRecorder()
	{
		return this.recorder!=null;
	}
	
	public Component getDtmfDetector()
	{
		if(this.dtmfDetector==null)	
		{
			this.dtmfDetector=resourcesPool.newAudioComponent(ComponentType.DTMF_DETECTOR);
			this.dtmfDetector.setEndpoint(endpoint);
			audioComponent.addOutput(((DetectorImpl)this.dtmfDetector).getAudioOutput());
			oobComponent.addOutput(((DetectorImpl)this.dtmfDetector).getOOBOutput());
			writeComponents++;
			writeDtmfComponents++;
			audioComponent.updateMode(readComponents!=0,true);
			oobComponent.updateMode(readDtmfComponents!=0,true);
			updateEndpoint(0,1);
		}
		
		return this.dtmfDetector;
	}
	
	public void releaseDtmfDetector()
	{
		if(this.dtmfDetector!=null)
		{
			audioComponent.remove(((DetectorImpl)this.dtmfDetector).getAudioOutput());
			oobComponent.remove(((DetectorImpl)this.dtmfDetector).getOOBOutput());
			writeComponents--;
			writeDtmfComponents--;
			audioComponent.updateMode(readComponents!=0,writeComponents!=0);			
			oobComponent.updateMode(readDtmfComponents!=0,writeDtmfComponents!=0);
			updateEndpoint(0,-1);
			this.dtmfDetector.clearEndpoint();			
			((DetectorImpl)this.dtmfDetector).clearAllListeners();
			this.dtmfDetector.deactivate();
			((DetectorImpl)this.dtmfDetector).clearBuffer();			
			resourcesPool.releaseAudioComponent(this.dtmfDetector,ComponentType.DTMF_DETECTOR);
			this.dtmfDetector=null;
		}
	}
	
	public boolean hasDtmfDetector()
	{
		return this.dtmfDetector!=null;
	}
	
	public Component getDtmfGenerator()
	{
		if(this.dtmfGenerator==null)
		{
			this.dtmfGenerator=resourcesPool.newAudioComponent(ComponentType.DTMF_GENERATOR);
			this.dtmfGenerator.setEndpoint(endpoint);
			audioComponent.addInput(((GeneratorImpl)this.dtmfGenerator).getAudioInput());
			oobComponent.addInput(((GeneratorImpl)this.dtmfGenerator).getOOBInput());
			readComponents++;
			readDtmfComponents++;
			audioComponent.updateMode(true,writeDtmfComponents!=0);
			updateEndpoint(1,0);
		}
		
		return this.dtmfGenerator;
	}
	
	public void releaseDtmfGenerator()
	{
		if(this.dtmfGenerator!=null)
		{
			audioComponent.remove(((GeneratorImpl)this.dtmfGenerator).getAudioInput());
			oobComponent.remove(((GeneratorImpl)this.dtmfGenerator).getOOBInput());
			readComponents--;
			readDtmfComponents--;
			oobComponent.updateMode(readDtmfComponents!=0,writeDtmfComponents!=0);			
			updateEndpoint(-1,0);
			this.dtmfGenerator.clearEndpoint();			
			this.dtmfGenerator.deactivate();
			resourcesPool.releaseAudioComponent(this.dtmfGenerator,ComponentType.DTMF_GENERATOR);
			this.dtmfGenerator=null;
		}
	}
	
	public boolean hasDtmfGenerator()
	{
		return this.dtmfGenerator!=null;
	}
	
	public Component getSignalDetector()
	{
		if(this.signalDetector==null)
		{
			this.signalDetector=resourcesPool.newAudioComponent(ComponentType.SIGNAL_DETECTOR);
			this.signalDetector.setEndpoint(endpoint);
			audioComponent.addOutput(((PhoneSignalDetector)this.signalDetector).getAudioOutput());
			writeComponents++;
			audioComponent.updateMode(readComponents!=0,true);
			updateEndpoint(0,1);			
		}
		
		return this.signalDetector;
	}
	
	public void releaseSignalDetector()
	{
		if(this.signalDetector!=null)
		{
			audioComponent.remove(((PhoneSignalDetector)this.signalDetector).getAudioOutput());
			writeComponents--;
			audioComponent.updateMode(readComponents!=0,writeComponents!=0);			
			updateEndpoint(0,-1);
			this.signalDetector.clearEndpoint();			
			((PhoneSignalDetector)this.signalDetector).clearAllListeners();
			this.signalDetector.deactivate();
			resourcesPool.releaseAudioComponent(this.signalDetector,ComponentType.SIGNAL_DETECTOR);
			this.signalDetector.deactivate();
			this.signalDetector=null;
		}
	}
	
	public boolean hasSignalDetector()
	{
		return this.signalDetector!=null;
	}
	
	public Component getSignalGenerator()
	{
		if(this.signalGenerator==null)
		{
			this.signalGenerator=resourcesPool.newAudioComponent(ComponentType.SIGNAL_GENERATOR);
			this.signalGenerator.setEndpoint(endpoint);
			audioComponent.addInput(((PhoneSignalGenerator)this.signalGenerator).getAudioInput());
			readComponents++;
			audioComponent.updateMode(true,writeComponents!=0);
			updateEndpoint(1,0);			
		}
		
		return this.signalGenerator;
	}
	
	public void releaseSignalGenerator()
	{
		if(this.signalGenerator!=null)
		{
			audioComponent.remove(((PhoneSignalGenerator)this.signalGenerator).getAudioInput());
			readComponents--;
			audioComponent.updateMode(readComponents!=0,writeComponents!=0);
			updateEndpoint(-1,0);
			this.signalGenerator.clearEndpoint();			
			this.signalGenerator.deactivate();
			resourcesPool.releaseAudioComponent(this.signalGenerator,ComponentType.SIGNAL_GENERATOR);
			this.signalGenerator=null;
		}
	}
	
	public boolean hasSignalGenerator()
	{
		return this.signalGenerator!=null;
	}
	
	private void updateEndpoint(int readChange,int writeChange)
	{
		boolean oldRead=(readComponents-readChange)!=0;
		boolean oldWrite=(writeComponents-writeChange)!=0;
		
		boolean newRead=readComponents!=0;
		boolean newWrite=writeComponents!=0;
		
		if(newRead==oldRead && newWrite==oldWrite)
			return;
		
		ConnectionMode oldMode,newMode;
		if(oldRead)
		{
			if(oldWrite)
				oldMode=ConnectionMode.CONFERENCE;
			else 
				oldMode=ConnectionMode.RECV_ONLY;
		}
		else if(oldWrite)
			oldMode=ConnectionMode.SEND_ONLY;
		else
			oldMode=ConnectionMode.INACTIVE;
		
		if(newRead)
		{
			if(newWrite)
				newMode=ConnectionMode.CONFERENCE;
			else 
				newMode=ConnectionMode.RECV_ONLY;
		}
		else if(newWrite)
			newMode=ConnectionMode.SEND_ONLY;
		else
			newMode=ConnectionMode.INACTIVE;
		
		endpoint.modeUpdated(oldMode,newMode);
	}
	
	public void releaseAll()
	{
		releasePlayer();
		releaseRecorder();
		releaseDtmfDetector();
		releaseDtmfGenerator();
		releaseSignalDetector();
		releaseSignalGenerator();
	}		
}
