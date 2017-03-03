/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.server.impl.rtp.channels;

import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Provides RTP, RTCP and LOCAL channels.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaChannelProvider {
    
    // Core Components
    private final ChannelsManager channelsManager;
    private final DspFactory dspFactory;
    
    public MediaChannelProvider(ChannelsManager channelsManager, DspFactory dspFactory) {
        // Core Components
        this.channelsManager = channelsManager;
        this.dspFactory = dspFactory;
    }
    
    public AudioChannel provideAudioChannel() {
        AudioChannel audioChannel = new AudioChannel(channelsManager.getClock(), channelsManager);
        try {
            audioChannel.setInputDsp(dspFactory.newProcessor());
            audioChannel.setOutputDsp(dspFactory.newProcessor());
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            // exception may happen only if invalid classes have been set in configuration
            throw new RuntimeException("There are invalid classes specified in the configuration.", e);
        }
        return audioChannel;
    }
    
    public String getLocalAddress() {
        return this.channelsManager.getBindAddress();
    }
    
    public String getExternalAddress() {
        return this.channelsManager.getExternalAddress();
    }

}
