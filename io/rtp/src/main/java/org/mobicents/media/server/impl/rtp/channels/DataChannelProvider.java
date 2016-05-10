/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.server.impl.rtp.channels;

import org.mobicents.media.server.impl.rtcp.RtcpChannel;
import org.mobicents.media.server.impl.rtp.LocalDataChannel;
import org.mobicents.media.server.impl.rtp.RtpChannel;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;

/**
 * Provides different type of channels to be used by MGCP connections.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface DataChannelProvider {

    /**
     * Provides an RTP channel.
     * 
     * @param statistics The object where channel statistics will be stored.
     * @param inbandClock A wall clock for in-band traffic.
     * @param outbandClock A wall clock for out-of-band traffic.
     * @return An RTP channel
     */
    RtpChannel provideRtpChannel(RtpStatistics statistics, RtpClock inbandClock, RtpClock outbandClock);

    /**
     * Provides and RTCP channel.
     * 
     * @param statistics The object where channel statistics will be stored.
     * @return An RTCP channel
     */
    RtcpChannel provideRtcpChannel(RtpStatistics statistics);

    /**
     * Provides a local data channel.
     * 
     * @return A local data channel.
     */
    LocalDataChannel provideLocalChannel();

}
