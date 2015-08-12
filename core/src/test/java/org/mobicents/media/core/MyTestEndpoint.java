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

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.endpoints.AbstractRelayEndpoint;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * 
 * @author yulian oifa
 */
public class MyTestEndpoint extends AbstractRelayEndpoint {

    private static final Logger LOGGER = Logger.getLogger(MyTestEndpoint.class);

    private int f;
    private Sine sine;
    private SpectraAnalyzer analyzer;
    private Component dtmfDetector;
    private Component dtmfGenerator;

    private InbandComponent inbandComponent;
    private OOBComponent oobComponent;

    public MyTestEndpoint(String localName, RelayType relayType, Dsp transcoder) {
        super(localName, relayType);
        inbandComponent = new InbandComponent(1, transcoder);
        oobComponent = new OOBComponent(-1);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
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

        inbandComponent.addInput(sine.getMediaInput());
        inbandComponent.addOutput(analyzer.getMediaOutput());
        this.dtmfDetector = resourcesPool.newAudioComponent(ComponentType.DTMF_DETECTOR);
        this.dtmfDetector.setEndpoint(this);

        inbandComponent.addOutput(((DetectorImpl) this.dtmfDetector).getMediaOutput());
        oobComponent.addOutput(((DetectorImpl) this.dtmfDetector).getOOBOutput());

        this.dtmfGenerator = resourcesPool.newAudioComponent(ComponentType.DTMF_GENERATOR);
        this.dtmfGenerator.setEndpoint(this);

        inbandComponent.addInput(((GeneratorImpl) this.dtmfGenerator).getMediaInput());
        oobComponent.addInput(((GeneratorImpl) this.dtmfGenerator).getOOBInput());

        audioRelay.addComponent(inbandComponent);
        oobRelay.addComponent(oobComponent);

        inbandComponent.setReadable(true);
        inbandComponent.setWritable(true);
        oobComponent.setReadable(true);
        oobComponent.setWritable(true);
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
}
