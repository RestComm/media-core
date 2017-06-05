/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.restcomm.media.rtp.rfc2833;

import java.util.ArrayList;
import java.util.List;

import org.restcomm.media.component.AbstractSource;
import org.restcomm.media.component.oob.OOBInput;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.format.DtmfFormat;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * Media source of RFC 2833 DTMF data coming from network.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class DtmfInput extends AbstractSource {

    private static final long serialVersionUID = 1648858097848867435L;

    // RTP Components
    private final RtpClock clock;
    private final OOBInput input;

    // Runtime Context
    private final List<Frame> frames;
    private Frame currFrame;

    private byte currTone;
    private int latestDuration;
    private int latestSequence;

    private boolean hasEndOfEvent;
    private long endTime;
    private int endSequence;

    private int eventDuration;
    private byte[] data;
    boolean endOfEvent;

    public DtmfInput(String name, PriorityQueueScheduler scheduler, RtpClock clock, OOBInput input) {
        super(name, scheduler, PriorityQueueScheduler.INPUT_QUEUE);

        // RTP Components
        this.clock = clock;
        this.clock.setClockRate(8000);
        this.input = input;
        this.connect(this.input);

        // Runtime Context
        this.frames = new ArrayList<Frame>(5);
        this.currFrame = null;
        this.currTone = (byte) 0xFF;
        this.latestDuration = 0;
        this.latestSequence = 0;
        this.hasEndOfEvent = false;
        this.endTime = 0;
        this.endSequence = 0;
        this.eventDuration = 0;
        this.data = new byte[4];
        this.endOfEvent = false;
    }

    public void write(RtpPacket event) {
        // obtain payload
        event.getPayload(data, 0);

        if (data.length == 0) {
            return;
        }

        boolean endOfEvent = false;
        if (data.length > 1) {
            endOfEvent = (data[1] & 0X80) != 0;
        }

        // lets ignore end of event packets
        if (endOfEvent) {
            this.hasEndOfEvent = true;
            this.endTime = event.getTimestamp();
            this.endSequence = event.getSequenceNumber();
            return;
        }

        this.eventDuration = (data[2] << 8) | (data[3] & 0xFF);

        // lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
        if (this.currTone == this.data[0]) {
            if (this.hasEndOfEvent) {
                if ((event.getSequenceNumber() <= this.endSequence && event.getSequenceNumber() > (this.endSequence - 8)))
                    // out of order , belongs to same event
                    // if comes after end of event then its new one
                    return;
            } else {
                if ((event.getSequenceNumber() < (this.latestSequence + 8))
                        && event.getSequenceNumber() > (this.latestSequence - 8)) {
                    if (event.getSequenceNumber() > this.latestSequence) {
                        this.latestSequence = event.getSequenceNumber();
                        this.latestDuration = this.eventDuration;
                    }

                    return;
                }

                if (this.eventDuration < (this.latestDuration + 1280) && this.eventDuration > (this.latestDuration - 1280)) {
                    if (this.eventDuration > this.latestDuration) {
                        this.latestSequence = event.getSequenceNumber();
                        this.latestDuration = this.eventDuration;
                    }
                    return;
                }
            }
        }

        this.hasEndOfEvent = false;
        this.endTime = 0;
        this.endSequence = 0;
        this.latestSequence = event.getSequenceNumber();
        this.latestDuration = this.eventDuration;
        this.currTone = this.data[0];

        for (int i = 0; i < 7; i++) {
            this.currFrame = Memory.allocate(DtmfFormat.PACKET_SIZE);
            byte[] newData = this.currFrame.getData();
            newData[0] = this.data[0];
            newData[1] = (byte) (0x3F & this.data[1]);
            this.eventDuration = (short) (160 * i);
            newData[2] = (byte) ((this.eventDuration >> 8) & 0xFF);
            newData[3] = (byte) (this.eventDuration & 0xFF);
            this.currFrame.setSequenceNumber(event.getSequenceNumber() + i);
            this.currFrame.setOffset(0);
            this.currFrame.setLength(DtmfFormat.PACKET_SIZE);
            this.currFrame.setFormat(DtmfFormat.FORMAT);
            this.currFrame.setDuration(DtmfFormat.PERIOD);
            this.currFrame.setTimestamp(clock.convertToAbsoluteTime(event.getTimestamp() + 20 * i));
            this.frames.add(this.currFrame);
        }

        for (int i = 7; i < 10; i++) {
            this.currFrame = Memory.allocate(DtmfFormat.PACKET_SIZE);
            byte[] newData = this.currFrame.getData();
            newData[0] = this.data[0];
            newData[1] = (byte) (0x80 | this.data[1]);
            this.eventDuration = (short) (160 * i);
            newData[2] = (byte) ((this.eventDuration >> 8) & 0xFF);
            newData[3] = (byte) (this.eventDuration & 0xFF);
            this.currFrame.setSequenceNumber(event.getSequenceNumber() + i);
            this.currFrame.setOffset(0);
            this.currFrame.setLength(DtmfFormat.PACKET_SIZE);
            this.currFrame.setFormat(DtmfFormat.FORMAT);
            this.currFrame.setDuration(DtmfFormat.PERIOD);
            this.currFrame.setTimestamp(this.clock.convertToAbsoluteTime(event.getTimestamp() + 20 * i));
            this.frames.add(this.currFrame);
        }
        wakeup();
    }

    @Override
    public Frame evolve(long timestamp) {
        if (this.frames.size() == 0) {
            return null;
        }
        return this.frames.remove(0);
    }

    @Override
    public void reset() {
        this.hasEndOfEvent = false;
        this.endTime = 0;
        this.endSequence = 0;
        this.latestSequence = 0;
        this.latestDuration = 0;
        this.currTone = (byte) 0xFF;
    }
}
