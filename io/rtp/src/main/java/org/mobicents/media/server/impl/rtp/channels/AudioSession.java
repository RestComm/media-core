/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.media.server.impl.rtp.channels;

import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpComponent;
import org.mobicents.media.server.impl.rtp.RtpTransport;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Media channel responsible for audio processing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class AudioSession extends RtpSession {

    public AudioSession(Scheduler scheduler, DspFactory dspFactory, UdpManager udpManager) {
        super(AVProfile.AUDIO, scheduler, udpManager);
        super.supportedFormats = AVProfile.audio;
        super.setFormats(this.supportedFormats);
    }

    private class RtpAudioComponent extends RtpComponent {

        public RtpAudioComponent(int componentId, Scheduler scheduler, DspFactory dspFactory, RtpTransport rtpTransport,
                RtpClock rtpClock, RtpClock oobClock, InbandComponent inbandComponent, OOBComponent ooBComponent) {
            super(componentId, scheduler, dspFactory, rtpTransport, rtpClock, oobClock, new AudioComponent(componentId),
                    new OOBComponent(componentId));
        }

    }

}
