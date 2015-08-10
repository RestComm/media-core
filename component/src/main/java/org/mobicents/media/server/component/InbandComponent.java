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

import java.util.Iterator;

import org.mobicents.media.server.component.audio.AudioOutput;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class InbandComponent {

    private final int componentId;
    private final ConcurrentMap<MediaInput> inputs;
    private final ConcurrentMap<MediaOutput> outputs;
    private boolean readable;
    private boolean writable;

    public InbandComponent(int componentId) {
        this.componentId = componentId;
        this.inputs = new ConcurrentMap<MediaInput>();
        this.outputs = new ConcurrentMap<MediaOutput>();
        this.readable = false;
        this.writable = false;
    }

    public int getComponentId() {
        return componentId;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public void addInput(MediaInput input) {
        this.inputs.put(input.getInputId(), input);
    }

    public void releaseInput(MediaInput input) {
        this.inputs.remove(input.getInputId());
    }

    public void addOutput(MediaOutput output) {
        this.outputs.put(output.getOutputId(), output);
    }

    public void releaseOutput(MediaOutput output) {
        this.outputs.remove(output.getOutputId());
    }

    public int[] retrieveData() {

    }

    public void submitData(int[] data, Format format) {
        if (this.writable) {
            // Allocate a new frame
            Frame frame = Memory.allocate(data.length);
            frame.setOffset(0);
            frame.setLength(PACKET_SIZE);
            frame.setDuration(PERIOD);
            frame.setFormat(LINEAR_FORMAT);

            int index = 0;
            int count = 0;
            byte[] payload = frame.getData();

            while (count < data.length) {
                payload[index++] = (byte) (data[count]);
                payload[index++] = (byte) (data[count++] >> 8);
            }

            // Send frame to all registered outputs
            Iterator<MediaOutput> activeOutputs = this.outputs.valuesIterator();
            while (activeOutputs.hasNext()) {
                MediaOutput output = activeOutputs.next();
                if (activeOutputs.hasNext()) {
                    output.offer(frame.clone());
                } else {
                    output.offer(frame);
                }
                output.wakeup();
            }
        }
    }

}
