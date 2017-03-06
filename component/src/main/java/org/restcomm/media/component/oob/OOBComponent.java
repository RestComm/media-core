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

package org.restcomm.media.component.oob;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.restcomm.media.concurrent.ConcurrentMap;
import org.restcomm.media.spi.memory.Frame;

/**
 * Implements compound components used by mixer and splitter.
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class OOBComponent {

    private final int componentId;
	private final ConcurrentMap<OOBInput> inputs;
	private final ConcurrentMap<OOBOutput> outputs;
	private final AtomicBoolean shouldRead;
	private final AtomicBoolean shouldWrite;

	private Frame frame;

	/**
	 * Creates new instance with default name.
	 */
	public OOBComponent(int componentId) {
		this.componentId = componentId;
		this.inputs = new ConcurrentMap<OOBInput>();
		this.outputs = new ConcurrentMap<OOBOutput>();
		this.shouldRead = new AtomicBoolean(false);
        this.shouldWrite = new AtomicBoolean(false);
	}

	public int getComponentId() {
		return componentId;
	}

	public void updateMode(boolean shouldRead, boolean shouldWrite) {
		this.shouldRead.set(shouldRead);
		this.shouldWrite.set(shouldWrite);
	}

	public void addInput(OOBInput input) {
		inputs.put(input.getInputId(), input);
	}

	public void addOutput(OOBOutput output) {
		outputs.put(output.getOutputId(), output);
	}

	public void remove(OOBInput input) {
		inputs.remove(input.getInputId());
	}

	public void remove(OOBOutput output) {
		outputs.remove(output.getOutputId());
	}

	public void perform() {
		frame = null;
		final Iterator<OOBInput> activeInputs = inputs.valuesIterator();
		while (activeInputs.hasNext()) {
			OOBInput input = activeInputs.next();
			frame = input.poll();
			if (frame != null) {
				break;
			}
		}
	}

	public Frame getData() {
		if (!this.shouldRead.get()) {
			if (frame != null) {
				frame.recycle();
			}
			return null;
		}
		return frame;
	}

	public void offer(Frame frame) {
		if (!this.shouldWrite.get()) {
			frame.recycle();
			return;
		}

		final Iterator<OOBOutput> activeOutputs = outputs.valuesIterator();
		while (activeOutputs.hasNext()) {
			OOBOutput output = activeOutputs.next();
			if (!activeOutputs.hasNext()) {
				output.offer(frame);
			} else {
				output.offer(frame.clone());
			}
			output.wakeup();
		}
	}
}
