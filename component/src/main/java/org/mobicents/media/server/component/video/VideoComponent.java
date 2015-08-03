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

package org.mobicents.media.server.component.video;

import org.mobicents.media.server.concurrent.ConcurrentMap;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class VideoComponent {

    private final int componentId;
    private final ConcurrentMap<VideoInput> inputs;
    private final ConcurrentMap<VideoOutput> outputs;
    private boolean readable;
    private boolean writable;

    public VideoComponent(int componentId) {
        this.componentId = componentId;
        this.inputs = new ConcurrentMap<VideoInput>();
        this.outputs = new ConcurrentMap<VideoOutput>();
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
    
    public void addInput(VideoInput input) {
        this.inputs.put(input.getInputId(), input);
    }

    public void removeInput(VideoInput input) {
        this.inputs.remove(input.getInputId());
    }

    public void addOutput(VideoOutput output) {
        this.outputs.put(output.getOutputId(), output);
    }
    
    public void removeOutput(VideoOutput output) {
        this.outputs.remove(output.getOutputId());
    }
    
    public int[] retrieveData() {
        int[] data = null;
        // TODO iterate over active inputs and retrieve a frame from each
        // TODO get the data from the frame and store it locally
        // TODO return the data
        return data;
    }
    
    public void offerData(int[] data) {
        // TODO allocate a new frame
        // TODO place the offered data into the frame
        // TODO offer the frame to all active outputs
    }

}
