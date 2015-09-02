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

import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.LinearFormats;

/**
 * Media channel responsible for audio processing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class AudioSession extends RtpSession {

    public AudioSession(int channelId, Scheduler scheduler, Processor transcoder, UdpManager udpManager) {
        super(channelId, AVProfile.AUDIO, LinearFormats.AUDIO, scheduler, transcoder, udpManager);
        this.supportedFormats = AVProfile.audio;
        setFormats(this.supportedFormats);
    }

    public AudioSession(int channelId, Scheduler scheduler, Processor transcoder, UdpManager udpManager, int jitterBufferSize) {
        super(channelId, AVProfile.AUDIO, LinearFormats.AUDIO, scheduler, transcoder, udpManager);
        this.supportedFormats = AVProfile.audio;
        setFormats(this.supportedFormats);
        setMaxJitterSize(jitterBufferSize);
    }

}
