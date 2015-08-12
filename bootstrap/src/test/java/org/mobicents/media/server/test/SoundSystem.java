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

package org.mobicents.media.server.test;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.endpoints.AbstractRelayEndpoint;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SoundCard;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author yulian oifa
 */
public class SoundSystem extends AbstractRelayEndpoint implements Endpoint {

    private static final Logger LOGGER = Logger.getLogger(SoundSystem.class);

    private int f;
    private Sine sine;
    private SoundCard soundcard;

    private InbandComponent inbandComponent;

    public SoundSystem(String localName, Dsp transcoder) {
        super(localName, RelayType.MIXER);
        inbandComponent = new InbandComponent(-1, transcoder);
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
        soundcard = new SoundCard(this.getScheduler());

        inbandComponent.addInput(sine.getMediaInput());
        inbandComponent.addOutput(soundcard.getMediaOutput());
        inbandComponent.setReadable(true);
        inbandComponent.setWritable(true);
        audioRelay.addComponent(inbandComponent);
        modeUpdated(ConnectionMode.INACTIVE, ConnectionMode.SEND_RECV);
    }

    public Component getResource(MediaType mediaType, ComponentType componentType) {
        switch (mediaType) {
            case AUDIO:
                switch (componentType) {
                    case SINE:
                        return sine;
                    case SOUND_CARD:
                        return soundcard;
                }
        }

        return null;
    }

    @Override
    public void releaseResource(MediaType mediaType, ComponentType componentType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
