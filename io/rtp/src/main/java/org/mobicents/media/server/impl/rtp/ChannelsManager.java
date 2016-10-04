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

package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.server.impl.rtcp.RtcpChannel;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.network.PortManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.sdp.format.AVProfile;
import org.mobicents.media.server.io.sdp.format.RTPFormats;
import org.mobicents.media.server.io.ss7.SS7DataChannel;
import org.mobicents.media.server.io.ss7.SS7Manager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;

/**
 * Local and RTP channels storage
 * Use for local and remote connections
 * 
 * @author yulian oifa
 */
public class ChannelsManager {
    //transport for RTP and RTCP
	private UdpManager udpManager;

	//ss7 manager
	private SS7Manager ss7Manager;
	
    private Clock clock = new WallClock();

    private boolean isControlEnabled=false;

    private PriorityQueueScheduler scheduler;
    
    private int jitterBufferSize=50;
    
    //channel id generator
    private AtomicInteger channelIndex = new AtomicInteger(100);
    
    private final RTPFormats codecs;
    
    /**
     * Creates a new channels manager with a subset of supported codecs.
     * 
     * @param udpManager The network manager.
     * @param codecs The list of supported codecs
     */
    public ChannelsManager(UdpManager udpManager, RTPFormats codecs) {
        this.udpManager = udpManager;
        this.codecs = codecs;
    }

    /**
     * Creates a new channels manager that supports every codec as assigned to {@link AVProfile#audio}.
     * 
     * @param udpManager The network manager.
     */
    public ChannelsManager(UdpManager udpManager) {
        this(udpManager, AVProfile.audio);
    }

    /**
     * Gets list of supported codecs
     * 
     * @return The collection of supported codecs.
     */
    public RTPFormats getCodecs() {
        return codecs;
    }

    public void setSS7Manager(SS7Manager ss7Manager) {
    	this.ss7Manager=ss7Manager;
    }
    
    public SS7Manager getSS7Manager() {
    	return this.ss7Manager;
    }
    
    public String getBindAddress() {
        return udpManager.getBindAddress();
    }

    public String getLocalBindAddress() {
        return udpManager.getLocalBindAddress();
    }
    
    public String getExternalAddress() {
    	return udpManager.getExternalAddress();
    }
    
    public PortManager getPortManager() {
    	return udpManager.getPortManager();
    }

    public void setScheduler(PriorityQueueScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public PriorityQueueScheduler getScheduler() {
        return this.scheduler;
    }
    
    public Clock getClock() {
        return clock;
    }

    public Boolean getIsControlEnabled() {
        return isControlEnabled;
    }
    
    public int getJitterBufferSize() {
    	return this.jitterBufferSize;
    }
    
    public void setJitterBufferSize(int jitterBufferSize) {
    	this.jitterBufferSize=jitterBufferSize;
    }        
    
    public UdpManager getUdpManager() {
    	return this.udpManager;
    }    
    
    @Deprecated
    public RTPDataChannel getChannel() {
        return new RTPDataChannel(this,channelIndex.incrementAndGet());
    }
    
    public RtpChannel getRtpChannel(RtpStatistics statistics, RtpClock clock, RtpClock oobClock) {
    	return new RtpChannel(channelIndex.incrementAndGet(), jitterBufferSize, statistics, clock, oobClock, scheduler, udpManager);
    }

    public RtcpChannel getRtcpChannel(RtpStatistics statistics) {
    	return new RtcpChannel(channelIndex.incrementAndGet(), statistics, udpManager);
    }
    
    public LocalDataChannel getLocalChannel() {
        return new LocalDataChannel(this, channelIndex.incrementAndGet());
    }
    
	public SS7DataChannel getSS7Channel(int dahdiChannelID, boolean isAlaw) throws IOException {
		if (ss7Manager == null) {
			throw new IOException("SS7 Not enabled");
		}
		return new SS7DataChannel(ss7Manager, dahdiChannelID, channelIndex.incrementAndGet(), isAlaw);
	}
    
    public AudioChannel getAudioChannel() {
    	return new AudioChannel(this.scheduler.getClock(), this);
    }
    
}
