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

package org.mobicents.media.server.component.audio;

import java.util.Iterator;

import org.mobicents.media.server.component.InbandInput;
import org.mobicents.media.server.component.InbandOutput;
import org.mobicents.media.server.component.MediaComponent;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Implements compound components used by mixer and splitter.
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class AudioComponent extends MediaComponent {

    // the format of the output stream.
    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    private Iterator<InbandInput> activeInputs;
    private Iterator<InbandOutput> activeOutputs;

    // samples storage
    private int[] data;

    private byte[] dataArray;
    private Frame inputFrame;
    private Frame outputFrame;

    int inputCount, outputCount, inputIndex, outputIndex;
    boolean first;

    /**
     * Creates new instance with default name.
     */
    public AudioComponent(int componentId) {
        super(componentId);
        this.data = new int[PACKET_SIZE / 2];
    }

    public void updateMode(boolean shouldRead, boolean shouldWrite) {
        this.readable = shouldRead;
        this.writable = shouldWrite;
    }

    @Override
    public int[] retrieveData() {
        if (this.readable) {
            this.first = true;
            this.activeInputs = inputs.valuesIterator();

            while (activeInputs.hasNext()) {
                InbandInput input = this.activeInputs.next();
                this.inputFrame = input.poll();
                if (this.inputFrame != null) {
                    this.dataArray = this.inputFrame.getData();
                    if (first) {
                        inputIndex = 0;
                        for (inputCount = 0; inputCount < dataArray.length; inputCount += 2) {
                            data[inputIndex++] = (short) (((dataArray[inputCount + 1]) << 8) | (dataArray[inputCount] & 0xff));
                        }
                        first = false;
                    } else {
                        inputIndex = 0;
                        for (inputCount = 0; inputCount < dataArray.length; inputCount += 2) {
                            data[inputIndex++] += (short) (((dataArray[inputCount + 1]) << 8) | (dataArray[inputCount] & 0xff));
                        }
                    }
                    inputFrame.recycle();
                }
            }
            return data;
        }
        return EMPTY_DATA;
    }

    @Override
    public void offerData(int[] data) {
        if (this.writable) {
            this.outputFrame = Memory.allocate(PACKET_SIZE);
            this.dataArray = outputFrame.getData();

            this.outputIndex = 0;
            for (outputCount = 0; outputCount < data.length;) {
                this.dataArray[outputIndex++] = (byte) (data[outputCount]);
                this.dataArray[outputIndex++] = (byte) (data[outputCount++] >> 8);
            }

            this.outputFrame.setOffset(0);
            this.outputFrame.setLength(PACKET_SIZE);
            this.outputFrame.setDuration(PERIOD);
            this.outputFrame.setFormat(LINEAR_FORMAT);

            this.activeOutputs = outputs.valuesIterator();
            while (this.activeOutputs.hasNext()) {
                InbandOutput output = activeOutputs.next();
                if (!activeOutputs.hasNext()) {
                    output.offer(outputFrame);
                } else {
                    output.offer(outputFrame.clone());
                }
                output.wakeup();
            }
        }
    }

}
