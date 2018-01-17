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

import com.google.common.util.concurrent.FutureCallback;
import org.restcomm.media.rtp.RtpInput;
import org.restcomm.media.rtp.RtpOutput;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionUpdateModeContext implements RtpSessionTransactionContext {

    private final FutureCallback<Void> callback;

    private final ConnectionMode mode;
    private final DtmfInput dtmfInput;
    private final RtpInput rtpInput;
    private final RtpOutput rtpOutput;

    public RtpSessionUpdateModeContext(ConnectionMode mode, DtmfInput dtmfInput, RtpInput rtpInput, RtpOutput rtpOutput, FutureCallback<Void> callback) {
        this.callback = callback;
        this.mode = mode;
        this.dtmfInput = dtmfInput;
        this.rtpInput = rtpInput;
        this.rtpOutput = rtpOutput;
    }

    @Override
    public FutureCallback<Void> getCallback() {
        return this.callback;
    }

    public ConnectionMode getMode() {
        return mode;
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
