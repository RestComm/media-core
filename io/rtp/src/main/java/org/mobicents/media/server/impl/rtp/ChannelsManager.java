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

import org.mobicents.media.server.impl.rtp.channels.AudioSession;
import org.mobicents.media.server.io.network.PortManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.ss7.SS7DataChannel;
import org.mobicents.media.server.io.ss7.SS7Manager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Local and RTP channels storage Use for local and remote connections
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class ChannelsManager {

    // Core elements
    private final Scheduler scheduler;
    private final UdpManager udpManager;
    private final DspFactory dspFactory;
    private SS7Manager ss7Manager;

    // Channels Manager properties
    private AtomicInteger channelIndex;
    private boolean isControlEnabled;
    private int jitterBufferSize;

    public ChannelsManager(Scheduler scheduler, UdpManager udpManager, DspFactory dspFactory) {
        // Core elements
        this.scheduler = scheduler;
        this.udpManager = udpManager;
        this.dspFactory = dspFactory;

        // Channels Manager properties
        this.channelIndex = new AtomicInteger(100);
        this.isControlEnabled = false;
        this.jitterBufferSize = 50;
    }

    public void setSS7Manager(SS7Manager ss7Manager) {
        this.ss7Manager = ss7Manager;
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

    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public Clock getClock() {
        return scheduler.getClock();
    }

    public Boolean getIsControlEnabled() {
        return isControlEnabled;
    }

    public int getJitterBufferSize() {
        return this.jitterBufferSize;
    }

    public void setJitterBufferSize(int jitterBufferSize) {
        this.jitterBufferSize = jitterBufferSize;
    }

    public UdpManager getUdpManager() {
        return this.udpManager;
    }

    public DspFactory getDspFactory() {
        return dspFactory;
    }

    @Deprecated
    public RTPDataChannel getChannel() {
        return new RTPDataChannel(this, channelIndex.incrementAndGet());
    }

    public LocalChannel getLocalChannel() {
        return new LocalChannel(this, channelIndex.incrementAndGet());
    }

    public SS7DataChannel getSS7Channel(int dahdiChannelID, boolean isAlaw) throws IOException {
        if (ss7Manager == null) {
            throw new IOException("SS7 Not enabled");
        }
        return new SS7DataChannel(ss7Manager, dahdiChannelID, channelIndex.incrementAndGet(), isAlaw);
    }

    public AudioSession getAudioChannel() {
        return new AudioSession(channelIndex.incrementAndGet(), scheduler, dspFactory.newProcessor(), udpManager,
                jitterBufferSize);
    }

}
