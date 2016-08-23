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

import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.sdp.format.AVProfile;
import org.mobicents.media.server.io.sdp.format.RTPFormats;
import org.mobicents.media.server.scheduler.Clock;

/**
 * Media channel responsible for audio processing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class AudioChannel extends MediaChannel {

	public static final String MEDIA_TYPE = "audio";

	public AudioChannel(Clock wallClock, ChannelsManager channelsManager) {
		super(MEDIA_TYPE, wallClock, channelsManager);
//		super.supportedFormats = super.buildRTPMap(AVProfile.audio);
		this.supportedFormats = new RTPFormats();
		String[] codecs = channelsManager.getCodecs();
		for(String codec : codecs) {
			
			switch (codec) {
			case "pcmu":
				supportedFormats.add(AVProfile.getFormat(0));
				break;
				
			case "pcma":
				supportedFormats.add(AVProfile.getFormat(8));
				break;
				
			case "gsm":
				supportedFormats.add(AVProfile.getFormat(3));
				break;
				
			case "g729":
				supportedFormats.add(AVProfile.getFormat(18));
				break;
				
			case "l16":
				supportedFormats.add(AVProfile.getFormat(97));
				break;				
				
			case "ilbc":
				supportedFormats.add(AVProfile.getFormat(102));
				break;
				
			case "linear":
				supportedFormats.add(AVProfile.getFormat(150));
				break;

			default:
				break;
			}
			
		}
		
		//Adding DTMF support
		supportedFormats.add(AVProfile.getFormat(101));
		supportedFormats.add(AVProfile.getFormat(126));
		
		super.setFormats(this.supportedFormats);
	}

	public AudioComponent getAudioComponent() {
		return this.rtpChannel.getAudioComponent();
	}

	public OOBComponent getAudioOobComponent() {
		return this.rtpChannel.getOobComponent();
	}

}
