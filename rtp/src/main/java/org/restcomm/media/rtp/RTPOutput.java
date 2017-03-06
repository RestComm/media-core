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

package org.restcomm.media.rtp;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.server.component.AbstractSink;
import org.restcomm.media.server.component.audio.AudioOutput;
import org.restcomm.media.spi.FormatNotSupportedException;
import org.restcomm.media.spi.dsp.Processor;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.format.Formats;
import org.restcomm.media.spi.memory.Frame;

/**
 * Transmitter implementation.
 * 
 * @author Yulian oifa
 */
public class RTPOutput extends AbstractSink {

	private static final long serialVersionUID = 3227885808614338323L;

	private static final Logger logger = Logger.getLogger(RTPOutput.class);

	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);

	@Deprecated
	private RTPDataChannel channel;

	private RtpTransmitter transmitter;

	// active formats
	private Formats formats;

	// signaling processor
	private Processor dsp;

	private AudioOutput output;

	/**
	 * Creates new transmitter
	 */
	@Deprecated
	protected RTPOutput(PriorityQueueScheduler scheduler, RTPDataChannel channel) {
		super("Output");
		this.channel = channel;
		output = new AudioOutput(scheduler, 1);
		output.join(this);
	}

	protected RTPOutput(PriorityQueueScheduler scheduler, RtpTransmitter transmitter) {
		super("Output");
		this.transmitter = transmitter;
		output = new AudioOutput(scheduler, 1);
		output.join(this);
	}

	public AudioOutput getAudioOutput() {
		return this.output;
	}

	@Override
	public void activate() {
		output.start();
	}

	@Override
	public void deactivate() {
		output.stop();
	}

	/**
	 * Assigns the digital signaling processor of this component. The DSP allows
	 * to get more output formats.
	 * 
	 * @param dsp
	 *            the dsp instance
	 */
	public void setDsp(Processor dsp) {
		this.dsp = dsp;
	}

	/**
	 * Gets the digital signaling processor associated with this media source
	 * 
	 * @return DSP instance.
	 */
	public Processor getDsp() {
		return this.dsp;
	}

	/**
	 * (Non Java-doc.)
	 * 
	 * 
	 * @see org.restcomm.media.MediaSink#setFormats(org.restcomm.media.spi.format.Formats)
	 */
	public void setFormats(Formats formats) throws FormatNotSupportedException {
		this.formats = formats;
	}

	@Override
	public void onMediaTransfer(Frame frame) throws IOException {
		// do transcoding
		if (dsp != null && formats != null && !formats.isEmpty()) {
			try {
				frame = dsp.process(frame, format, formats.get(0));
			} catch (Exception e) {
				// transcoding error , print error and try to move to next frame
				logger.error(e.getMessage(), e);
				return;
			}
		}

		if (this.transmitter != null) {
			this.transmitter.send(frame);
		}

		// XXX deprecated code
		if (this.channel != null) {
			channel.send(frame);
		}

	}
}
