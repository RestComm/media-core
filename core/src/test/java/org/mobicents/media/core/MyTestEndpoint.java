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

package org.mobicents.media.core;

import java.util.Random;

import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.endpoints.BaseMixerEndpoint;
import org.mobicents.media.server.component.audio.MediaComponent;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * 
 * @author yulian oifa
 */
public class MyTestEndpoint extends BaseMixerEndpoint {
    
    private static final Random RANDOM = new Random();

    private int f;
    private Sine sine;
    private SpectraAnalyzer analyzer;
    private Component dtmfDetector;
    private Component dtmfGenerator;
    private MediaComponent mediaComponent;

    public MyTestEndpoint(String localName) {
        super(localName);
        this.mediaComponent = new MockMediaComponent(999);
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

        mediaComponent.addAudioInput(sine.getAudioInput());
        mediaComponent.addAudioOutput(analyzer.getAudioOutput());

        this.dtmfDetector = resourcesPool.newAudioComponent(ComponentType.DTMF_DETECTOR);
        this.dtmfDetector.setEndpoint(this);

        mediaComponent.addAudioOutput(((DetectorImpl) this.dtmfDetector).getAudioOutput());
        mediaComponent.addOOBOutput(((DetectorImpl) this.dtmfDetector).getOOBOutput());

        this.dtmfGenerator = resourcesPool.newAudioComponent(ComponentType.DTMF_GENERATOR);
        this.dtmfGenerator.setEndpoint(this);

        mediaComponent.addAudioInput(((GeneratorImpl) this.dtmfGenerator).getAudioInput());
        mediaComponent.addOOBInput(((GeneratorImpl) this.dtmfGenerator).getOOBInput());
        mediaComponents.put(RANDOM.nextInt(), mediaComponent);

        audioMixer.addComponent(mediaComponent.getAudioComponent());
        oobMixer.addComponent(mediaComponent.getOOBComponent());
        mediaComponent.updateMode(ConnectionMode.SEND_RECV);
        modeUpdated(ConnectionMode.INACTIVE, ConnectionMode.SEND_RECV);
    }

    public Component getResource(MediaType mediaType, ComponentType componentType) {
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

    private class MockMediaComponent extends MediaComponent {

        public MockMediaComponent(int channelId) {
            super(channelId);
        }

    }
}
