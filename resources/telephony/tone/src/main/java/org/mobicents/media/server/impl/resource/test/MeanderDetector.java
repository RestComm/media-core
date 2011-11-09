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

package org.mobicents.media.server.impl.resource.test;

import java.io.IOException;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.spi.dsp.Codec;

/**
 *
 * @author kulikov
 */
public class MeanderDetector extends AbstractSink {

    private final static Format[] FORMATS = new Format[]{Codec.LINEAR_AUDIO};
    private int sampleRate;
    private double T;
    private short A;
    private short[] localBuffer;
    private int size;
    private int offset;
    private int count;
    private long seq;
    private boolean first = true;
    
    public MeanderDetector(String name) {
        super(name);
        sampleRate = (int) Codec.LINEAR_AUDIO.getSampleRate();
    }

    public void setPeriod(double T) {
        this.T = T / 2;
        //preparing local buffer
        //we have to hold at least 3-4 periods of data for analysis
        size = (int) (sampleRate * this.T);
        localBuffer = new short[4 * size];
    }

    public void setAmplitude(short A) {
        this.A = A;
    }

    public Format[] getFormats() {
        return FORMATS;
    }

    public boolean isAcceptable(Format format) {
        return format.matches(Codec.LINEAR_AUDIO);
    }

    @Override
    public void start() {
        seq = 0;
        first = true;
        super.start();
    }

    private boolean checkShock(short a, short b) {
        if (a == 0) {
            return b == A;
        } else if (a == A) {
            return b == 0;
        }
        return false;
    }

    private void analysis() {
        int p1 = 0;
        for (int i = 0; i < localBuffer.length - 1; i++) {
            if (localBuffer[i] != localBuffer[i + 1]) {
                int delta = i - p1;
                p1 = i;
                if (Math.abs(delta - size) < 2) {
                    count++;
                    if (count == 3 && checkShock(localBuffer[i], localBuffer[i + 1])) {
                        count = 0;
                        sendEvent(new MeanderEvent(this, MeanderEvent.EVENT_MEANDER));
                        break;
                    }
                }
            }
        }
    }

    public void onMediaTransfer(Buffer buffer) throws IOException {
        //checking sequence number
        if (!first && (buffer.getSequenceNumber() - seq) != 1) {
            sendEvent(new MeanderEvent(this, MeanderEvent.EVENT_OUT_OF_SEQUENCE));
        }
        seq = buffer.getSequenceNumber();

        if (first) {
            first = false;
        }
        
        if (!buffer.getFormat().matches(Codec.LINEAR_AUDIO)) {
            sendEvent(new MeanderEvent(this, MeanderEvent.EVENT_FORMAT_MISSMATCH));
        }

        //checking data
        byte[] data = buffer.getData();
        for (int i = 0; i < buffer.getLength() - 1; i += 2) {
            if (offset < localBuffer.length) {
                short v = (short) ((data[i + buffer.getOffset()] & 0xff) | (data[i + 1 + buffer.getOffset()] << 8));
                localBuffer[offset++] = v;//(short) ((data[i + buffer.getOffset()] & 0xff) | (data[i + 1 + buffer.getOffset()] << 8));
            } else {
                analysis();
                offset = 0;
            }
        }
    }
}
