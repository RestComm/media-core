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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.media.control.mgcp.endpoint;

import org.restcomm.media.Component;
import org.restcomm.media.ComponentType;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.Sine;
import org.restcomm.media.component.audio.SpectraAnalyzer;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.control.mgcp.endpoint.BaseMixerEndpointImpl;
import org.restcomm.media.resource.dtmf.DetectorImpl;
import org.restcomm.media.resource.dtmf.GeneratorImpl;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.ResourceUnavailableException;

/**
 * 
 * @author yulian oifa
 */
public class MyTestEndpoint extends BaseMixerEndpointImpl {

	private int f;
	private Sine sine;
	private SpectraAnalyzer analyzer;
	private Component dtmfDetector;
	private Component dtmfGenerator;

	private AudioComponent audioComponent;
	private OOBComponent oobComponent;

	public MyTestEndpoint(String localName) {
		super(localName);
		audioComponent = new AudioComponent(1);
		oobComponent = new OOBComponent(-1);
	}

	public void setFreq(int f) {
		this.f = f;
	}

	@Override
	public void start() throws ResourceUnavailableException {
		super.start();

		sine = new Sine(this.getScheduler());
		sine.setFrequency(f);
		sine.setAmplitude((short) (Short.MAX_VALUE / 3));
		analyzer = new SpectraAnalyzer("analyzer", this.getScheduler());

		audioComponent.addInput(sine.getAudioInput());
		audioComponent.addOutput(analyzer.getAudioOutput());
		this.dtmfDetector = resourcesPool.newAudioComponent(ComponentType.DTMF_DETECTOR);

		audioComponent.addOutput(((DetectorImpl) this.dtmfDetector).getAudioOutput());
		oobComponent.addOutput(((DetectorImpl) this.dtmfDetector).getOOBOutput());

		this.dtmfGenerator = resourcesPool.newAudioComponent(ComponentType.DTMF_GENERATOR);

		audioComponent.addInput(((GeneratorImpl) this.dtmfGenerator).getAudioInput());
		oobComponent.addInput(((GeneratorImpl) this.dtmfGenerator).getOOBInput());

		audioMixer.addComponent(audioComponent);
		oobMixer.addComponent(oobComponent);

		audioComponent.updateMode(true, true);
		oobComponent.updateMode(true, true);
		modeUpdated(ConnectionMode.INACTIVE, ConnectionMode.SEND_RECV);
	}

	public Component getResource(MediaType mediaType,
			ComponentType componentType) {
		switch (mediaType) {
		case AUDIO:
			switch (componentType) {
			case SINE:
				return sine;
			case SPECTRA_ANALYZER:
				return analyzer;
			case DTMF_GENERATOR:
				return dtmfGenerator;
			case DTMF_DETECTOR:
				return dtmfDetector;
			default:
				break;
			}
		default:
			break;
		}
		return null;
	}

	@Override
	public void releaseResource(MediaType mediaType, ComponentType componentType) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

    @Override
    public String toString() {
        return "MyTest";
    }
}
