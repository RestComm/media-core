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

import org.restcomm.media.rtp.JitterBuffer;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpInput;
import org.restcomm.media.rtp.RtpOutput;
import org.restcomm.media.rtp.rfc2833.DtmfInput;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionCloseContext extends RtpSessionBaseTransactionContext {

    private final RtpChannel channel;
    private final JitterBuffer jitterBuffer;
    private final DtmfInput dtmfInput;
    private final RtpInput rtpInput;
    private final RtpOutput rtpOutput;

    public RtpSessionCloseContext(RtpChannel channel, JitterBuffer jitterBuffer, DtmfInput dtmfInput, RtpInput rtpInput, RtpOutput rtpOutput, FutureCallback<Void> callback) {
        super(callback);
        this.channel = channel;
        this.jitterBuffer = jitterBuffer;
        this.dtmfInput = dtmfInput;
        this.rtpInput = rtpInput;
        this.rtpOutput = rtpOutput;
    }

    public RtpChannel getChannel() {
        return channel;
    }

    public JitterBuffer getJitterBuffer() {
        return jitterBuffer;
    }

    public DtmfInput getDtmfInput() {
        return dtmfInput;
    }

    public RtpInput getRtpInput() {
        return rtpInput;
    }

    public RtpOutput getRtpOutput() {
        return rtpOutput;
    }

}
