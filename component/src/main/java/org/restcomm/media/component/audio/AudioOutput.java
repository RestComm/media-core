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

package org.restcomm.media.component.audio;

import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.component.AbstractSource;
import org.restcomm.media.concurrent.ConcurrentCyclicFIFO;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.memory.Frame;

/**
 * Implements output for compound components.
 * 
 * @author Yulian Oifa
 */
public class AudioOutput extends AbstractSource {

	private static final long serialVersionUID = -5988244809612104056L;

	private int outputId;
	private ConcurrentCyclicFIFO<Frame> buffer = new ConcurrentCyclicFIFO<Frame>();

	/**
	 * Creates new instance with default name.
	 */
	public AudioOutput(PriorityQueueScheduler scheduler, int outputId) {
		super("compound.output", scheduler, PriorityQueueScheduler.OUTPUT_QUEUE);
		this.outputId = outputId;
	}

	public int getOutputId() {
		return outputId;
	}

	public void join(AbstractSink sink) {
		connect(sink);
	}

	public void unjoin() {
		disconnect();
	}

	@Override
	public Frame evolve(long timestamp) {
		return buffer.poll();
	}

	@Override
	public void stop() {
		while (buffer.size() > 0) {
			Frame frame = buffer.poll();
			if(frame != null) {
			    frame.recycle();
			}
		}
		super.stop();
	}

	public void resetBuffer() {
		while (buffer.size() > 0) {
			buffer.poll().recycle();
		}
	}

	public void offer(Frame frame) {
		if (buffer.size() > 1) {
			buffer.poll().recycle();
		}
		buffer.offer(frame);
	}
}
