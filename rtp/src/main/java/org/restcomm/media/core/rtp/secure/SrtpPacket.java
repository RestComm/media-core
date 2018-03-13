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

package org.restcomm.media.core.rtp.secure;

import org.restcomm.media.core.rtp.RtpPacket;

/**
 * Sub-type of {@link RtpPacket} that simply indicates that payload is secured.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SrtpPacket extends RtpPacket {

    private static final long serialVersionUID = -3626195561478320074L;

    public SrtpPacket(boolean allocateDirect) {
        this(RtpPacket.RTP_PACKET_MAX_SIZE, allocateDirect);
    }

    public SrtpPacket(int capacity, boolean allocateDirect) {
        super(capacity, allocateDirect);
    }

}
