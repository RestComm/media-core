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

package org.restcomm.media.core.rtp;

import java.io.IOException;

import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBInput;
import org.restcomm.media.component.oob.OOBOutput;
import org.restcomm.media.core.spi.ConnectionMode;
import org.restcomm.media.core.spi.ModeNotSupportedException;
import org.restcomm.media.core.spi.format.AudioFormat;
import org.restcomm.media.core.spi.format.FormatFactory;


/**
 * Local Channel implementation.
 * Bridge between 2 endpoints.
 * 
 * @author Oifa Yulian
 */
public class LocalDataChannel {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
	private long period = 20000000L;
	private int packetSize = (int) (period / 1000000) * format.getSampleRate() / 1000 * format.getSampleSize() / 8;

	private AudioComponent audioComponent;
	private AudioInput input;
	private AudioOutput output;

	private OOBComponent oobComponent;
	private OOBInput oobInput;
	private OOBOutput oobOutput;

	private LocalDataChannel otherChannel = null;

	/**
	 * Creates new local channel.
	 */
	public LocalDataChannel(ChannelsManager channelsManager, int channelId) {
		audioComponent = new AudioComponent(channelId);
		input = new AudioInput(1, packetSize);
		output = new AudioOutput(channelsManager.getScheduler(), 2);
		audioComponent.addInput(input);
		audioComponent.addOutput(output);

		oobComponent = new OOBComponent(channelId);
		oobInput = new OOBInput(1);
		oobOutput = new OOBOutput(channelsManager.getScheduler(), 2);
		oobComponent.addInput(oobInput);
		oobComponent.addOutput(oobOutput);
	}

	public AudioInput getAudioInput() {
		return this.input;
	}

	public AudioOutput getAudioOutput() {
		return this.output;
	}

	public AudioComponent getAudioComponent() {
		return this.audioComponent;
	}

	public OOBInput getOOBInput() {
		return this.oobInput;
	}

	public OOBOutput getOOBOutput() {
		return this.oobOutput;
	}

	public OOBComponent getOOBComponent() {
		return this.oobComponent;
	}

	public void join(LocalDataChannel otherChannel) throws IOException {
		if (this.otherChannel != null) {
			throw new IOException("Channel already joined");
		}

		this.otherChannel = otherChannel;
		otherChannel.otherChannel = this;
		this.otherChannel.getAudioOutput().join(input);
		this.otherChannel.getOOBOutput().join(oobInput);
		output.join(this.otherChannel.getAudioInput());
		oobOutput.join(this.otherChannel.getOOBInput());
	}

	public void unjoin() {
		if (this.otherChannel == null) {
			return;
		}

		this.output.deactivate();
		this.oobOutput.deactivate();
		output.unjoin();
		oobOutput.unjoin();

		this.otherChannel = null;
	}

	public void updateMode(ConnectionMode connectionMode) throws ModeNotSupportedException {
		if (this.otherChannel == null) {
			throw new ModeNotSupportedException("You should join channel first");
		}

		switch (connectionMode) {
		case SEND_ONLY:
			audioComponent.updateMode(false, true);
			oobComponent.updateMode(false, true);
			output.activate();
			oobOutput.activate();
			break;
		case RECV_ONLY:
			audioComponent.updateMode(true, false);
			oobComponent.updateMode(true, false);
			output.deactivate();
			oobOutput.deactivate();
			break;
		case INACTIVE:
			audioComponent.updateMode(false, false);
			oobComponent.updateMode(false, false);
			output.deactivate();
			oobOutput.deactivate();
			break;
		case SEND_RECV:
		case CONFERENCE:
			audioComponent.updateMode(true, true);
			oobComponent.updateMode(true, true);
			output.activate();
			oobOutput.activate();
			break;
		case NETWORK_LOOPBACK:
			throw new ModeNotSupportedException("Loopback not supported on local channel");
		default:
			// XXX handle default case
			break;
		}
	}
}