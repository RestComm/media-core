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

package org.restcomm.media.rtp.session;

import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionIncomingRtpContext extends RtpSessionBaseTransactionContext {

    private final RtpPacket packet;
    private final JitterBuffer jitterBuffer;
    private final DtmfInput dtmfInput;

    public RtpSessionIncomingRtpContext(RtpPacket packet, JitterBuffer jitterBuffer, DtmfInput dtmfInput) {
        super(null);
        this.packet = packet;
        this.jitterBuffer = jitterBuffer;
        this.dtmfInput = dtmfInput;
    }

    public RtpPacket getPacket() {
        return packet;
    }

    public JitterBuffer getJitterBuffer() {
        return jitterBuffer;
    }

    public DtmfInput getDtmfInput() {
        return dtmfInput;
    }

}
