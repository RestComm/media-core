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
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;

/**
 * This is the starting point for creating, maintaining
 * and closing an RTP session.
 * 
 * @author kulikov
 */
public class RTPManager {
    //transport for RTP and RTCP
    protected UdpManager udpManager;

    private Clock clock = new DefaultClock();

    /** Jitter value*/
    protected int jitter = 60;

    /** Bind address */
    private String bindAddress;

    /** Available port range */
    private int lowPort = 1024;
    private int highPort = 65535;

    protected boolean isControlEnabled;

    protected Scheduler scheduler;
    
    //RFC2833 dtmf payload
    //by default disabled
    protected int dtmf = -1;
    
    public RTPManager(UdpManager udpManager) {
        this.udpManager = udpManager;
        this.bindAddress = udpManager.getBindAddress();
    }

    /**
     * Gets the IP address to which RTP is bound.
     *
     * @return the IP address as character string
     */
    public String getBindAddress() {
        return udpManager.getBindAddress();
    }

    /**
     * Modify the bind address.
     *
     * @param bindAddress the IP address as string or host name.
     * @deprecated 
     */
    public void setBindAddress(String bindAddress) {
        this.udpManager.setBindAddress(bindAddress);
    }
    
    /**
     * Gets the IP address to which RTP is bound.
     *
     * @return the IP address as character string
     */
    public String getLocalBindAddress() {
        return udpManager.getLocalBindAddress();
    }

    /**
     * Modify the bind address.
     *
     * @param bindAddress the IP address as string or host name.
     * @deprecated 
     */
    public void setLocalBindAddress(String bindAddress) {
        this.udpManager.setLocalBindAddress(bindAddress);
    }

    /**
     * Gets the minimum available port number.
     *
     * @return port number
     */
    public int getLowPort() {
        return lowPort;
    }

    /**
     * Modifies minimum available port
     *
     * @param lowPort the port number.
     */
    public void setLowPort(int lowPort) {
        this.lowPort = lowPort;
    }

    /**
     * Gets the maximum available port number.
     *
     * @return port number
     */
    public int getHighPort() {
        return highPort;
    }

    /**
     * Modifies maximum available port
     *
     * @param port the port number.
     */
    public void setHighPort(int port) {
        this.highPort = port;
    }

    public void setJitter(int jitter) {
        this.jitter = jitter;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Modifies payload id for RFC2833 DTMF event.
     * 
     * @param dtmf the payload id or -1 if disabled.
     */
    public void setDtmf(int dtmf) {
        this.dtmf = dtmf;
    }
    
    public void start() throws IOException {
    }

    public void stop() {        
    }

    public Clock getClock() {
        return clock;
    }

    public RTPDataChannel getChannel() throws IOException {
        return new RTPDataChannel(this);
    }
}
