/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.dsp;

import java.io.IOException;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.spi.dsp.Attenuator;
import org.mobicents.media.server.spi.dsp.SignalingProcessor;

/**
 * 
 * @author amit bhayani
 * 
 */
public class AttenuatorImpl extends BaseComponent implements
		SignalingProcessor, Attenuator {

	private final static AudioFormat LINEAR_AUDIO = new AudioFormat(
			AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN,
			AudioFormat.SIGNED);

	private final static Format[] FORMATS = new Format[] { LINEAR_AUDIO };

	private Input input;
	private Output output;

	private long timestamp;

	private Buffer buff;

	private int volume = 0;

	private double factor = 1d;

	public AttenuatorImpl(String name) {
		super(name);
		input = new Input(name);
		output = new Output(name);
	}

	public void setVolume(int volume) {
		this.volume = volume;
		
		if(this.volume > 0){
			throw new IllegalArgumentException("Attenuator volume cannot be greater than zero");
		}

		this.factor = Math.pow(10, (this.volume / 10d));
	}

	public int getVolume() {
		return this.volume;
	}

	public void connect(MediaSource source) {
		input.connect(source);
	}

	public void disconnect(MediaSource source) {
		input.disconnect(source);
	}

	public MediaSink getInput() {
		return this.input;
	}

	public void connect(MediaSink sink) {
		output.connect(sink);
	}

	public void disconnect(MediaSink sink) {
		output.disconnect(sink);
	}

	public MediaSource getOutput() {
		return this.output;
	}

	/**
	 * (Non Java-doc).
	 * 
	 * @see org.mobicents.media.Component#start()
	 */
	public void start() {

		//System.out.println("Attenuator Started");

		// start input and output channel
		if (!input.isStarted()) {
			input.start();
		}
	}

	private class Input extends AbstractSink {
		private volatile boolean started = false;

		public Input(String name) {
			super(name + ".input");
		}

		@Override
		public boolean isStarted() {
			return this.started;
		}

		@Override
		public void start() {
			this.started = true;
			if (!output.isStarted()) {
				output.start();
			}
		}

		@Override
		public void stop() {
			this.started = false;
			if (output.isStarted()) {
				output.stop();
			}
		}

		@Override
		public void onMediaTransfer(Buffer buffer) throws IOException {
			timestamp = buffer.getTimeStamp();
			output.transmit(buffer);
			//System.out.println("Attenuator.Input.onMediaTransfer()");
		}

		public long getTimestamp() {
			return timestamp;
		}

		public Format[] getFormats() {
			return FORMATS;
		}

		@Override
		public String toString() {
			return "Attenuator.Input[" + getName() + "]";
		}

	}

	private class Output extends AbstractSource {

		private volatile boolean started = false;

		public Output(String name) {
			super(name + ".output");
		}

		@Override
		public void evolve(Buffer buffer, long timestamp) {
			//System.out.println("Attenuator.Output.evolve");
			buffer.copy(buff);

		}

		public Format[] getFormats() {
			return FORMATS;
		}

		@Override
		public boolean isStarted() {
			return this.started;
		}

		@Override
		public void start() {
			this.started = true;
			if (!input.isStarted()) {
				input.start();
			}
			super.start();
		}

		@Override
		public void stop() {
			this.started = false;
			if (input.isStarted()) {
				input.stop();
			}
			super.stop();
		}

		protected void transmit(Buffer buffer) {
			if (!started) {
				buffer.dispose();
				return;
			}

			// Discard packet silently if output handler is not assigned yet
			if (otherParty == null) {
				buffer.dispose();
				return;
			}

			// deliver packet to the consumer
			buff = buffer;

			byte[] data = buff.getData();
			for (int i = 0; i < buff.getLength(); i+=2) {
				
				short s = (short) ((data[i] & 0xff) | (data[i + 1] << 8));
				s = (short)(s*factor);
				
		        data[i] = (byte) s;
		        data[i+1] = (byte) (s >> 8);
			}
			perform();
		}

		@Override
		public String toString() {
			return "Attenuator.Output[" + getName() + "]";
		}

	}

}
