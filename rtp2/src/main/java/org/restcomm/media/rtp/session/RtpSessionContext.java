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

import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionContext {

    private final long ssrc;
    private final MediaType mediaType;
    private final RtpSessionStatistics statistics;
    private final RTPFormats supportedFormats;

    private SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private RTPFormats negotiatedFormats;
    private ConnectionMode mode;

    public RtpSessionContext(long ssrc, MediaType mediaType, RtpSessionStatistics statistics, RTPFormats formats) {
        super();
        this.ssrc = ssrc;
        this.mediaType = mediaType;
        this.statistics = statistics;
        this.supportedFormats = formats;

        this.negotiatedFormats = new RTPFormats();
        this.mode = ConnectionMode.INACTIVE;
    }

    public long getSsrc() {
        return ssrc;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public RtpSessionStatistics getStatistics() {
        return statistics;
    }

    public RTPFormats getSupportedFormats() {
        return supportedFormats;
    }

    public RTPFormats getNegotiatedFormats() {
        return negotiatedFormats;
    }

    public void setNegotiatedFormats(RTPFormats negotiatedFormats) {
        this.negotiatedFormats = negotiatedFormats;
    }

    public boolean isFormatSupported(int payloadType) {
        return this.negotiatedFormats.contains(payloadType);
    }

    public ConnectionMode getMode() {
        return mode;
    }

    public void setMode(ConnectionMode mode) {
        this.mode = mode;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

}
