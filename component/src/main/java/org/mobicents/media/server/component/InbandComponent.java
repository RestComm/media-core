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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class InbandComponent {

    private static final Frame[] EMPTY_DATA = new Frame[0];

    private final int componentId;
    private final ConcurrentMap<MediaInput> inputs;
    private final ConcurrentMap<MediaOutput> outputs;
    private final Processor transcoder;
    private boolean readable;
    private boolean writable;

    public InbandComponent(int componentId, Processor transcoder) {
        this.componentId = componentId;
        this.inputs = new ConcurrentMap<MediaInput>();
        this.outputs = new ConcurrentMap<MediaOutput>();
        this.transcoder = transcoder;
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

    /**
     * Retrieves data from each input registered in the component.<br>
     * The media relay (mixer or translator) that receives the data will decide whether to mix the frames from each
     * synchronization source or to simply forward them.
     * 
     * @param format The expected format of the retrieved frames. May force the component to perform transcoding.<br>
     *        If null, the frames will retain their original format.
     * 
     * @return An array containing the most recent frame of each input. Return an empty array if no data is available or the
     *         component is not readable.
     */
    public Frame[] retrieveData(Format format) {
        if (this.readable && !this.inputs.isEmpty()) {
            List<Frame> frames = new ArrayList<Frame>(this.inputs.size());
            Iterator<MediaInput> activeInputs = this.inputs.valuesIterator();

            while (activeInputs.hasNext()) {
                MediaInput input = activeInputs.next();
                Frame frame = input.poll();

                if (frame != null) {
                    // perform transcoding if necessary
                    if (format != null && !frame.getFormat().matches(format)) {
                        frame = this.transcoder.process(frame, frame.getFormat(), format);
                    }
                    frames.add(frame);
                }
            }
            return frames.toArray(new Frame[frames.size()]);
        }
        return EMPTY_DATA;
    }

    /**
     * Retrieves data from each input registered in the component <b>maintaing the original format</b> (no transcoding
     * involved).<br>
     * The media relay (mixer or translator) that receives the data will decide whether to mix the frames from each
     * synchronization source or to simply forward them.
     * 
     * @return An array containing the most recent frame of each input. Return an empty array if no data is available or the
     *         component is not readable.
     */
    public Frame[] retrieveData() {
        return retrieveData(null);
    }

    /**
     * Submits data to be broadcast amongst all registered outputs.<br>
     * If the source of the data is a mixer, then a single frame will be submitted.<br>
     * Otherwise, if the source is a translator, multiple frames may be offered since they are forwarded from the
     * synchronization source.
     * 
     * @param frames The array of frames to be offered to the outputs.
     */
    public void submitData(Frame... frames) {
        if (this.writable && !this.outputs.isEmpty() && frames.length > 0) {
            for (Frame frame : frames) {
                // Send frame to all registered outputs
                Iterator<MediaOutput> activeOutputs = this.outputs.valuesIterator();
                while (activeOutputs.hasNext()) {
                    MediaOutput output = activeOutputs.next();
                    System.out.println("Component " + componentId + " is offering data to output " + output.getOutputId());
                    if (activeOutputs.hasNext()) {
                        output.offer(frame.clone());
                    } else {
                        output.offer(frame);
                    }
                }
            }

            // wake up outputs to restore synchronization
            Iterator<MediaOutput> activeOutputs = this.outputs.valuesIterator();
            while (activeOutputs.hasNext()) {
                activeOutputs.next().wakeup();
            }
        }
    }

}
