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

import org.mobicents.media.server.concurrent.ConcurrentMap;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class MediaComponent {

    protected static final int[] EMPTY_DATA = new int[0];

    protected final int componentId;
    protected final ConcurrentMap<InbandInput> inputs;
    protected final ConcurrentMap<InbandOutput> outputs;

    protected boolean readable;
    protected boolean writable;

    public MediaComponent(int componentId) {
        this.componentId = componentId;
        this.inputs = new ConcurrentMap<InbandInput>();
        this.outputs = new ConcurrentMap<InbandOutput>();
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

    public void addInput(InbandInput input) {
        this.inputs.put(input.getInputId(), input);
    }

    public void removeInput(InbandInput input) {
        this.inputs.remove(input.getInputId());
    }

    public void addOutput(InbandOutput output) {
        this.outputs.put(output.getOutputId(), output);
    }

    public void removeOutput(InbandOutput output) {
        this.outputs.remove(output.getOutputId());
    }

    public abstract int[] retrieveData();

    public abstract void offerData(int[] data);

}
