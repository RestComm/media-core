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

package org.restcomm.media.control.mgcp.connection.local;

import java.io.IOException;

import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBInput;
import org.restcomm.media.component.oob.OOBOutput;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.ModeNotSupportedException;


/**
 * Data channel that connects two local endpoints.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class LocalDataChannel {

	private final AudioComponent component;
	private final AudioInput input;
	private final AudioOutput output;

	private final OOBComponent oobComponent;
	private final OOBInput oobInput;
	private final OOBOutput oobOutput;

	private LocalDataChannel otherChannel = null;

	public LocalDataChannel(AudioComponent inbandComponent, AudioInput inbandInput, AudioOutput inbandOutput, OOBComponent oobComponent, OOBInput oobInput, OOBOutput oobOutput) {
		this.component = inbandComponent;
		this.input = inbandInput;
		this.output = inbandOutput;
		
		this.component.addInput(input);
		this.component.addOutput(output);

		this.oobComponent = oobComponent;
		this.oobInput = oobInput;
		this.oobOutput = oobOutput;
		
		this.oobComponent.addInput(oobInput);
		this.oobComponent.addOutput(oobOutput);
	}

	public AudioInput getInbandInput() {
		return this.input;
	}

	public AudioOutput getInbandOutput() {
		return this.output;
	}

	public AudioComponent getInbandComponent() {
		return this.component;
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
		this.otherChannel.getInbandOutput().join(input);
		this.otherChannel.getOOBOutput().join(oobInput);
		output.join(this.otherChannel.getInbandInput());
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
			component.updateMode(false, true);
			oobComponent.updateMode(false, true);
			output.activate();
			oobOutput.activate();
			break;
		case RECV_ONLY:
			component.updateMode(true, false);
			oobComponent.updateMode(true, false);
			output.deactivate();
			oobOutput.deactivate();
			break;
		case INACTIVE:
			component.updateMode(false, false);
			oobComponent.updateMode(false, false);
			output.deactivate();
			oobOutput.deactivate();
			break;
		case SEND_RECV:
		case CONFERENCE:
			component.updateMode(true, true);
			oobComponent.updateMode(true, true);
			output.activate();
			oobOutput.activate();
			break;
		case NETWORK_LOOPBACK:
		default:
			throw new ModeNotSupportedException("Loopback not supported on local channel");
		}
	}
}