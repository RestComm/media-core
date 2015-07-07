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

import java.util.ArrayList;

import org.mobicents.media.server.component.oob.OOBInput;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Media source for out-of-band DTMF.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DtmfSource extends AbstractSource {

    private static final long serialVersionUID = -8378447263318686944L;

    private static final String SOURCE_NAME = "dtmfconverter";

    // Linear mixing format
    private static final int PACKET_SIZE = 4;
    private static final int CLOCK_RATE = 8000;
    private static final long PERIOD = 20000000L;
    private static final AudioFormat DTMF_FORMAT = FormatFactory.createAudioFormat("telephone-event", CLOCK_RATE);

    // Media mixer components
    private final RtpClock oobClock;
    private final OOBInput ooBinput;

    // DTMF processing
    private final ArrayList<Frame> frameBuffer;
    private Frame currFrame;
    private byte currTone;
    private int latestDuration;
    private int latestSeq;
    private boolean hasEndOfEvent;
    private long endTime;
    private int endSeq;
    private int eventDuration;
    private byte[] data;

    public DtmfSource(Scheduler scheduler, RtpClock oobClock) {
        super(SOURCE_NAME, scheduler, Scheduler.INPUT_QUEUE);

        // Media mixer components
        this.oobClock = oobClock;
        this.oobClock.setClockRate(CLOCK_RATE);
        this.ooBinput = new OOBInput(2);
        this.connect(ooBinput);

        // DTMF processing
        this.frameBuffer = new ArrayList<Frame>(5);
        this.data = new byte[4];
        this.currTone = (byte) 0xFF;
        this.latestDuration = 0;
        this.latestSeq = 0;
        this.hasEndOfEvent = false;
        this.endTime = 0;
        this.endSeq = 0;
    }

    public OOBInput getOoBinput() {
        return ooBinput;
    }

    public void write(RtpPacket packet) {
        // obtain payload - if no data exists then exit
        packet.getPayload(this.data, 0);
        if (this.data.length == 0) {
            return;
        }

        boolean endOfEvent = false;
        if (this.data.length > 1) {
            endOfEvent = (data[1] & 0X80) != 0;
        }

        // ignore end of event packets
        if (endOfEvent) {
            this.hasEndOfEvent = true;
            this.endTime = packet.getTimestamp();
            this.endSeq = packet.getSeqNumber();
            return;
        }

        this.eventDuration = (this.data[2] << 8) | (this.data[3] & 0xFF);

        // lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
        if (this.currTone == data[0]) {
            if (this.hasEndOfEvent) {
                if ((packet.getSeqNumber() <= this.endSeq && packet.getSeqNumber() > (this.endSeq - 8)))
                    // out of order , belongs to same event if comes after end of event then its new one
                    return;
            } else {
                if ((packet.getSeqNumber() < (this.latestSeq + 8)) && packet.getSeqNumber() > (this.latestSeq - 8)) {
                    if (packet.getSeqNumber() > this.latestSeq) {
                        this.latestSeq = packet.getSeqNumber();
                        this.latestDuration = this.eventDuration;
                    }
                    return;
                }

                if (this.eventDuration < (this.latestDuration + 1280) && this.eventDuration > (this.latestDuration - 1280)) {
                    if (this.eventDuration > this.latestDuration) {
                        this.latestSeq = packet.getSeqNumber();
                        this.latestDuration = eventDuration;
                    }
                    return;
                }
            }
        }

        this.hasEndOfEvent = false;
        this.endTime = 0;
        this.endSeq = 0;
        this.latestSeq = packet.getSeqNumber();
        this.latestDuration = eventDuration;
        this.currTone = data[0];

        for (int i = 0; i < 7; i++) {
            this.currFrame = Memory.allocate(PACKET_SIZE);
            byte[] newData = this.currFrame.getData();
            newData[0] = this.data[0];
            newData[1] = (byte) (0x3F & this.data[1]);
            this.eventDuration = (short) (160 * i);
            newData[2] = (byte) ((this.eventDuration >> 8) & 0xFF);
            newData[3] = (byte) (this.eventDuration & 0xFF);
            this.currFrame.setSequenceNumber(packet.getSeqNumber() + i);
            this.currFrame.setOffset(0);
            this.currFrame.setLength(PACKET_SIZE);
            this.currFrame.setFormat(DTMF_FORMAT);
            this.currFrame.setDuration(PERIOD);
            this.currFrame.setTimestamp(this.oobClock.convertToAbsoluteTime(packet.getTimestamp() + 20 * i));
            this.frameBuffer.add(this.currFrame);
        }

        for (int i = 7; i < 10; i++) {
            this.currFrame = Memory.allocate(PACKET_SIZE);
            byte[] newData = this.currFrame.getData();
            newData[0] = this.data[0];
            newData[1] = (byte) (0x80 | this.data[1]);
            this.eventDuration = (short) (160 * i);
            newData[2] = (byte) ((this.eventDuration >> 8) & 0xFF);
            newData[3] = (byte) (this.eventDuration & 0xFF);
            this.currFrame.setSequenceNumber(packet.getSeqNumber() + i);
            this.currFrame.setOffset(0);
            this.currFrame.setLength(PACKET_SIZE);
            this.currFrame.setFormat(DTMF_FORMAT);
            this.currFrame.setDuration(PERIOD);
            this.currFrame.setTimestamp(this.oobClock.convertToAbsoluteTime(packet.getTimestamp() + 20 * i));
            this.frameBuffer.add(this.currFrame);
        }

        wakeup();
    }

    @Override
    public Frame evolve(long timestamp) {
        return (frameBuffer.size() == 0) ? null : frameBuffer.remove(0);
    }

    @Override
    public void reset() {
        this.hasEndOfEvent = false;
        this.endTime = 0;
        this.endSeq = 0;
        this.latestSeq = 0;
        this.latestDuration = 0;
        this.currTone = (byte) 0xFF;
    }
}
