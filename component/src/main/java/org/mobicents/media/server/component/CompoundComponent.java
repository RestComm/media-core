/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.component;

import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioInput;
import org.mobicents.media.server.component.audio.AudioOutput;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.component.oob.OOBInput;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * Contains the media and out-of-band components of a connection.
 * 
 * @author Henrique Rosa (henrique.rosa@gmail.com)
 *
 */
public class CompoundComponent {

    private final int channelId;
    private final AudioComponent audioComponent;
    private final OOBComponent ooBComponent;

    public CompoundComponent(int channelId) {
        this.channelId = channelId;
        this.audioComponent = new AudioComponent(channelId);
        this.ooBComponent = new OOBComponent(channelId);
    }

    public int getChannelId() {
        return channelId;
    }
    
    public AudioComponent getAudioComponent() {
        return audioComponent;
    }

    public void addAudioInput(AudioInput input) {
        this.audioComponent.addInput(input);
    }

    public void addAudioOutput(AudioOutput output) {
        this.audioComponent.addOutput(output);
    }

    public OOBComponent getOOBComponent() {
        return ooBComponent;
    }

    public void addOOBInput(OOBInput input) {
        this.ooBComponent.addInput(input);
    }

    public void addOOBOutput(OOBOutput output) {
        this.ooBComponent.addOutput(output);
    }

    public void updateMode(ConnectionMode connectionMode) {
        switch (connectionMode) {
            case SEND_ONLY:
                audioComponent.updateMode(false, true);
                ooBComponent.updateMode(false, true);
                break;
            case RECV_ONLY:
                audioComponent.updateMode(true, false);
                ooBComponent.updateMode(true, false);
                break;
            case INACTIVE:
                audioComponent.updateMode(false, false);
                ooBComponent.updateMode(false, false);
                break;
            case SEND_RECV:
            case CONFERENCE:
                audioComponent.updateMode(true, true);
                ooBComponent.updateMode(true, true);
                break;
            case NETWORK_LOOPBACK:
                audioComponent.updateMode(false, false);
                ooBComponent.updateMode(false, false);
                break;
            default:
                break;
        }
    }

}
