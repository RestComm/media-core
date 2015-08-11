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

package org.mobicents.media.server.io.ss7;

import java.io.IOException;
import java.util.ArrayList;

import org.mobicents.media.hardware.dahdi.Channel;
import org.mobicents.media.server.component.MediaOutput;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.dtmf.DtmfTonesData;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 *
 * @author Oifa Yulian
 */
/**
 * Transmitter implementation.
 *
 */
public class SS7Output extends AbstractSink {

    private static final long serialVersionUID = -242059116640028724L;

    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    private AudioFormat destinationFormat;

    private Channel channel;

    private Sender sender;

    // signaling processor
    private Processor dsp;

    private Scheduler scheduler;

    // The underlying buffer size for both audio and oob
    private static final int QUEUE_SIZE = 5;
    private static final int OOB_QUEUE_SIZE = 5;

    // the underlying buffer for both audio and oob
    private final ArrayList<Frame> queue = new ArrayList<Frame>(QUEUE_SIZE);
    private final ArrayList<Frame> oobQueue = new ArrayList<Frame>(OOB_QUEUE_SIZE);

    // number of bytes to send in single cycle
    private static final int SEND_SIZE = 32;

    private MediaOutput output;

    private OOBOutput oobOutput;
    private OOBTranslator oobTranslator;

    /**
     * Creates new transmitter
     */
    protected SS7Output(Scheduler scheduler, Channel channel, AudioFormat destinationFormat) {
        super("Output");
        this.channel = channel;
        this.destinationFormat = destinationFormat;
        this.sender = new Sender();

        this.scheduler = scheduler;
        output = new MediaOutput(1, scheduler);
        output.join(this);

        oobOutput = new OOBOutput(scheduler, 1);
        oobTranslator = new OOBTranslator();
        oobOutput.join(oobTranslator);
    }

    public MediaOutput getAudioOutput() {
        return this.output;
    }

    public OOBOutput getOOBOutput() {
        return this.oobOutput;
    }

    @Override
    public void activate() {
        output.start();
        oobOutput.start();
    }

    @Override
    public void deactivate() {
        output.stop();
        oobOutput.stop();
    }

    /**
     * Assigns the digital signaling processor of this component. The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(Processor dsp) {
        // assign processor
        this.dsp = dsp;
    }

    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public Processor getDsp() {
        return this.dsp;
    }

    @Override
    public void start() {
        super.start();
        sender.submit();
    }

    @Override
    public void stop() {
        super.stop();
        sender.cancel();
    }

    public void setDestinationFormat(AudioFormat destinationFormat) {
        this.destinationFormat = destinationFormat;
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        // do transcoding
        if (dsp != null && destinationFormat != null) {
            try {
                frame = dsp.process(frame, LINEAR_FORMAT, destinationFormat);
            } catch (Exception e) {
                // transcoding error , print error and try to move to next frame
                System.out.println(e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        if (queue.size() >= QUEUE_SIZE)
            queue.remove(0);

        queue.add(frame);
    }

    private class Sender extends Task {
        private Frame currFrame = null;
        private byte[] smallBuffer = new byte[SEND_SIZE];
        int framePosition = 0;
        int readCount = 0;

        public Sender() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return Scheduler.SENDER_QUEUE;
        }

        public void submit() {
            this.activate(false);
            scheduler.submit(this, Scheduler.SENDER_QUEUE);
        }

        @Override
        public long perform() {
            if (currFrame == null) {
                if (oobQueue.size() > 0) {
                    currFrame = oobQueue.remove(0);
                    framePosition = 0;
                } else if (queue.size() > 0) {
                    currFrame = queue.remove(0);
                    framePosition = 0;
                }
            }

            readCount = 0;
            if (currFrame != null) {
                byte[] data = currFrame.getData();
                if (framePosition + SEND_SIZE < data.length) {
                    System.arraycopy(data, framePosition, smallBuffer, 0, 32);
                    readCount = SEND_SIZE;
                    framePosition += SEND_SIZE;
                } else if (framePosition < data.length - 1) {
                    System.arraycopy(data, framePosition, smallBuffer, 0, data.length - framePosition);
                    readCount = data.length - framePosition;
                    currFrame.recycle();
                    currFrame = null;
                    framePosition = 0;
                }
            }

            while (readCount < smallBuffer.length)
                smallBuffer[readCount++] = (byte) 0;

            try {
                channel.write(smallBuffer, readCount);
            } catch (IOException e) {
            }

            scheduler.submit(this, Scheduler.SENDER_QUEUE);
            return 0;
        }
    }

    private class OOBTranslator extends AbstractSink {

        private static final long serialVersionUID = 6242552334258611251L;

        private byte currTone = (byte) 0xFF;
        private long latestSeq = 0;
        private int seqNumber = 1;

        private boolean hasEndOfEvent = false;
        private long endSeq = 0;

        public OOBTranslator() {
            super("oob translator");
        }

        @Override
        public void onMediaTransfer(Frame buffer) throws IOException {
            byte[] data = buffer.getData();
            if (data.length != 4)
                return;

            boolean endOfEvent = false;
            endOfEvent = (data[1] & 0X80) != 0;

            // lets ignore end of event packets
            if (endOfEvent) {
                hasEndOfEvent = true;
                endSeq = buffer.getSequenceNumber();
                return;
            }

            // lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
            if (currTone == data[0]) {
                if (hasEndOfEvent) {
                    if (buffer.getSequenceNumber() <= endSeq && buffer.getSequenceNumber() > (endSeq - 8))
                        // out of order , belongs to same event
                        // if comes after end of event then its new one
                        return;
                } else if ((buffer.getSequenceNumber() < (latestSeq + 8)) && buffer.getSequenceNumber() > (latestSeq - 8)) {
                    if (buffer.getSequenceNumber() > latestSeq)
                        latestSeq = buffer.getSequenceNumber();

                    return;
                }
            }

            hasEndOfEvent = false;
            endSeq = 0;

            latestSeq = buffer.getSequenceNumber();
            currTone = data[0];

            for (int i = 0; i < 5; i++) {
                Frame currFrame = Memory.allocate(PACKET_SIZE);
                currFrame.setHeader(null);
                currFrame.setSequenceNumber(seqNumber++);
                currFrame.setTimestamp(System.currentTimeMillis());
                currFrame.setLength(PACKET_SIZE);
                currFrame.setDuration(20000000L);
                currFrame.setOffset(0);
                currFrame.setLength(PACKET_SIZE);
                System.arraycopy(DtmfTonesData.buffer[data[0]], i * PACKET_SIZE, currFrame.getData(), 0, PACKET_SIZE);

                if (dsp != null && destinationFormat != null) {
                    try {
                        currFrame = dsp.process(currFrame, LINEAR_FORMAT, destinationFormat);
                    } catch (Exception e) {
                        // transcoding error , print error and try to move to next frame
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                }
                oobQueue.add(currFrame);
            }
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }
}
