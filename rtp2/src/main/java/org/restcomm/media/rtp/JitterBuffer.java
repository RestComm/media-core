/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.rtp;

import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.spi.memory.Frame;

/**
 * A jitter buffer temporarily stores arriving packets in order to minimize delay variations. If packets arrive too late then
 * they are discarded. A jitter buffer may be mis-configured and be either too large or too small.
 * 
 * <p>
 * If a jitter buffer is too small then an excessive number of packets may be discarded, which can lead to call quality
 * degradation. If a jitter buffer is too large then the additional delay can lead to conversational difficulty.
 * </p>
 * <p>
 * A typical jitter buffer configuration is 30mS to 50mS in size. In the case of an adaptive jitter buffer then the maximum size
 * may be set to 100-200mS. Note that if the jitter buffer size exceeds 100mS then the additional delay introduced can lead to
 * conversational difficulty.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface JitterBuffer extends JitterBufferSubject {

    /**
     * Offers a packet to the jitter buffer.
     * 
     * @param packet The RTP packet
     * @param format The format of the RTP packet
     */
    void write(RtpPacket packet, RTPFormat format);

    /**
     * Consumes a frame from the jitter buffer.
     * 
     * @param timestamp
     * @return The next ordered frame in the jitter buffer.
     */
    Frame read(long timestamp);

    /**
     * Sets whether the buffer is active or not.
     * 
     * @param inUse
     */
    void setInUse(boolean inUse);

    /**
     * Restarts the jitter buffer.
     */
    void restart();

}
