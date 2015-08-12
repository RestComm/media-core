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

package org.mobicents.media.server.component;

import java.io.IOException;

import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Implements input for compound components.
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaInput extends AbstractSink {

    private static final long serialVersionUID = 7744459545593089374L;

    private static final String NAME_PREFIX = "compound.input.";

    private static final int PASSTHROUGH = 0;
    private static final int BUFFER_SIZE = 3;

    // Input properties
    private final int inputId;
    private final ConcurrentCyclicFIFO<Frame> buffer;
    private int bufferSize;

    // Media processing (runtime)
    private Frame activeFrame = null;
    private byte[] activeData;
    private byte[] oldData;
    private int byteIndex;
    private int packetSize;

    public MediaInput(int inputId, int packetSize) {
        super(NAME_PREFIX + inputId);

        // Input properties
        this.inputId = inputId;
        this.bufferSize = BUFFER_SIZE;
        this.buffer = new ConcurrentCyclicFIFO<Frame>();

        // Media processing
        this.byteIndex = 0;
        this.packetSize = packetSize;
    }

    public MediaInput(int inputId) {
        this(inputId, PASSTHROUGH);
    }

    public int getInputId() {
        return inputId;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize < 0 ? 0 : packetSize;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Indicates the state of the input buffer.
     *
     * @return true if input buffer has no frames.
     */
    public boolean isEmpty() {
        return this.buffer.size() == 0;
    }

    /**
     * Retrieves frame from the input buffer.
     *
     * @return the media frame.
     */
    public Frame poll() {
        return buffer.poll();
    }

    /**
     * Recycles input stream
     */
    public void recycle() {
        while (this.buffer.size() > 0) {
            this.buffer.poll().recycle();
        }

        if (this.activeFrame != null) {
            this.activeFrame.recycle();
        }

        this.activeFrame = null;
        this.activeData = null;
        this.byteIndex = 0;
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        if (this.packetSize == PASSTHROUGH) {
            forwardFrame(frame);
        } else {
            aggregateFrame(frame);
        }
    }

    private void aggregateFrame(Frame frame) {
        // generate frames with correct size here, aggregate frames if needed.
        // allows to accept several sources with different ptime (packet time)
        this.oldData = frame.getData();

        int count = 0;
        while (count < oldData.length) {
            if (activeData == null) {
                activeFrame = Memory.allocate(packetSize);
                activeFrame.setOffset(0);
                activeFrame.setLength(packetSize);
                activeFrame.setFormat(frame.getFormat());
                activeData = activeFrame.getData();
                byteIndex = 0;
            }

            if (oldData.length - count < activeData.length - byteIndex) {
                System.arraycopy(oldData, count, activeData, byteIndex, oldData.length - count);
                byteIndex += oldData.length - count;
                count = oldData.length;
            } else {
                System.arraycopy(oldData, count, activeData, byteIndex, activeData.length - byteIndex);
                count += activeData.length - byteIndex;

                if (buffer.size() >= bufferSize) {
                    buffer.poll().recycle();
                }
                buffer.offer(activeFrame);
                System.out.println("Input " + this.hashCode() + " buffer size: " + buffer.size());

                activeFrame = null;
                activeData = null;
            }
        }
        frame.recycle();
    }

    private void forwardFrame(Frame frame) {
        buffer.offer(frame);
    }

    @Override
    public void activate() {
        // Does nothing
    }

    @Override
    public void deactivate() {
        // Does nothing
    }

}
