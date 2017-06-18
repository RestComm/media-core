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

package org.restcomm.media.rtp;

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.rtcp.RtcpChannel;
import org.restcomm.media.rtp.channels.AudioChannel;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.jitter.JitterBufferFactory;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormats;

/**
 * Local and RTP channels storage
 * Use for local and remote connections
 * 
 * @author yulian oifa
 */
public class ChannelsManager {
    //transport for RTP and RTCP
	private UdpManager udpManager;

    private Clock clock = new WallClock();

    private boolean isControlEnabled=false;

    private PriorityQueueScheduler scheduler;
    
    private int jitterBufferSize=50;
    
    //channel id generator
    private AtomicInteger channelIndex = new AtomicInteger(100);
    
    private final RTPFormats codecs;
    private DtlsSrtpServerProvider dtlsServerProvider;
    private JitterBufferFactory jitterBufferFactory;
    
    /**
     * Creates a new channels manager with a subset of supported codecs.
     * 
     * @param udpManager The network manager.
     * @param codecs The list of supported codecs
     * @param dtlsServerProvider The provider of DtlsSrtpServer instances
     */
    public ChannelsManager(UdpManager udpManager, RTPFormats codecs, DtlsSrtpServerProvider dtlsServerProvider, JitterBufferFactory jitterBufferFactory) {
        this.udpManager = udpManager;
        this.codecs = codecs;
        this.dtlsServerProvider = dtlsServerProvider;
        this.jitterBufferFactory = jitterBufferFactory;
    }

    /**
     * Creates a new channels manager that supports every codec as assigned to {@link AVProfile#audio}.
     * 
     * @param udpManager The network manager.
     * @param dtlsServerProvider The provider of DtlsSrtpServer instances
     */
    public ChannelsManager(UdpManager udpManager, DtlsSrtpServerProvider dtlsServerProvider, JitterBufferFactory jitterBufferFactory) {
        this(udpManager, AVProfile.audio, dtlsServerProvider, jitterBufferFactory);
    }

    /**
     * Gets list of supported codecs
     * 
     * @return The collection of supported codecs.
     */
    public RTPFormats getCodecs() {
        return codecs;
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
        return new RTPDataChannel(this,channelIndex.incrementAndGet(), this.dtlsServerProvider);
    }
    
    public RtpChannel getRtpChannel(RtpStatistics statistics, RtpClock clock, RtpClock oobClock) {
    	return new RtpChannel(channelIndex.incrementAndGet(), statistics, clock, oobClock, scheduler, udpManager, dtlsServerProvider, jitterBufferFactory);
    }

    public RtcpChannel getRtcpChannel(RtpStatistics statistics) {
        return new RtcpChannel(channelIndex.incrementAndGet(), statistics, udpManager, dtlsServerProvider);
    }
    
    public LocalDataChannel getLocalChannel() {
        return new LocalDataChannel(this, channelIndex.incrementAndGet());
    }
    
    public AudioChannel getAudioChannel() {
    	return new AudioChannel(this.scheduler.getClock(), this);
    }

    public JitterBufferFactory getJitterBufferFactory() {
        return jitterBufferFactory;
    }

    public void setJitterBufferFactory(JitterBufferFactory jitterBufferFactory) {
        this.jitterBufferFactory = jitterBufferFactory;
    }

   
    
    
}
