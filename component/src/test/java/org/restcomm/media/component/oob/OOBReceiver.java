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

import java.io.IOException;

import org.restcomm.media.ComponentType;
import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.component.oob.OOBOutput;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.memory.Frame;

/**
 * 
 * @author yulian oifa
 */
public class OOBReceiver extends AbstractSink {

	private static final long serialVersionUID = -1186889768370720579L;

	private int packetSize = 4;
	private int count = 0;

	private OOBOutput output;

	public OOBReceiver(String name, PriorityQueueScheduler scheduler) {
		super(name);
		output = new OOBOutput(scheduler, ComponentType.SPECTRA_ANALYZER.getType());
		output.join(this);
	}

	public OOBOutput getOOBOutput() {
		return this.output;
	}

	@Override
	public void activate() {
		this.count = 0;
		output.start();
	}

	@Override
	public void deactivate() {
		output.stop();
	}

	@Override
	public void onMediaTransfer(Frame frame) throws IOException {
		if (frame.getLength() == packetSize) {
			count++;
		}
	}

	public int getPacketsCount() {
		return this.count;
	}
	
}
