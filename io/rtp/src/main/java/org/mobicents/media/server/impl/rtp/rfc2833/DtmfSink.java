/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.impl.rtp.rfc2833;

import java.io.IOException;

import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpComponent;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Media sink for out-of-band DTMF
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Yulian oifa
 */
public class DtmfSink extends AbstractSink {

    private static final long serialVersionUID = 8004138073890646792L;

    private static final String SINK_NAME = "Output";

    // Media mixer components
    private final OOBOutput oobOutput;

    // RTP transport
    private final RtpComponent rtpComponent;
    private final RtpPacket dtmfPacket;
    private final RtpClock oobClock;

    // Details of a transmitted packet
    private long dtmfTimestamp;

    public DtmfSink(Scheduler scheduler, RtpComponent rtpGateway, RtpClock oobClock) {
        super(SINK_NAME);

        // Media mixer components
        oobOutput = new OOBOutput(scheduler, 1);
        oobOutput.join(this);

        // RTP transport
        this.rtpComponent = rtpGateway;
        this.dtmfPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
        this.oobClock = oobClock;

        // Details of a transmitted packet
        this.dtmfTimestamp = -1;
    }

    public OOBOutput getOobOutput() {
        return oobOutput;
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        if (evolve(frame)) {
            this.rtpComponent.outgoingDtmf(this.dtmfPacket);
        }
    }

    private boolean evolve(Frame frame) {
        // ignore frames with duplicate time stamp
        if (frame.getTimestamp() / 1000000L == this.dtmfTimestamp) {
            frame.recycle();
            return false;
        }

        // convert to milliseconds first, then to rtp time units
        this.dtmfTimestamp = frame.getTimestamp() / 1000000L;
        this.dtmfTimestamp = this.oobClock.convertToRtpTime(dtmfTimestamp);

        // wrap the DTMF packet
        // NOTE: the SSRC field is unknown at this point, it must be overwritten by the RTP transport object!
        // NOTE: the sequence number field is unknown at this point, it must be overwritten by the RTP transport object!
        this.dtmfPacket.wrap(false, AVProfile.telephoneEventsID, 0, dtmfTimestamp, 0L, frame.getData(), frame.getOffset(),
                frame.getLength());
        frame.recycle();
        return true;
    }

    @Override
    public void activate() {
        this.oobOutput.start();
    }

    @Override
    public void deactivate() {
        this.oobOutput.stop();
    }

}
