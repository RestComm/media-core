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

import java.net.SocketAddress;

import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.sdp.format.RTPFormats;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Transaction Context for {@link RtpSessionEvent#NEGOTIATE} event.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionNegotiateContext extends RtpSessionBaseTransactionContext {

    // RTP Components
    private final RtpChannel channel;

    // Remote Peer
    private final RTPFormats formats;
    private final SocketAddress address;
    private final long ssrc;

    public RtpSessionNegotiateContext(RtpChannel channel, RTPFormats formats, SocketAddress address, long ssrc, FutureCallback<Void> callback) {
        super(callback);

        // RTP Components
        this.channel = channel;

        // Remote Peer
        this.formats = formats;
        this.address = address;
        this.ssrc = ssrc;
    }

    public RtpChannel getChannel() {
        return channel;
    }

    public RTPFormats getFormats() {
        return formats;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public long getSsrc() {
        return ssrc;
    }

}
