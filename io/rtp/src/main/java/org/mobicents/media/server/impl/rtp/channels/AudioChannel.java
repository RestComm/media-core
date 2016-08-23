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

import java.util.Iterator;

import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.sdp.format.RTPFormat;
import org.mobicents.media.server.io.sdp.format.RTPFormats;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.spi.format.FormatFactory;

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
		//super.supportedFormats = AVProfile.audio;
		this.supportedFormats = new RTPFormats();
		Iterator<String> codecs = channelsManager.getCodecs();
		while(codecs.hasNext()) {
			
			switch (codecs.next()) {
			case "pcmu":
				RTPFormat pcmu = new RTPFormat(0, FormatFactory.createAudioFormat("pcmu", 8000, 8, 1), 8000);
				supportedFormats.add(pcmu);
				break;
				
			case "pcma":
				RTPFormat pcma = new RTPFormat(8, FormatFactory.createAudioFormat("pcma", 8000, 8, 1), 8000);
				supportedFormats.add(pcma);
				break;
				
			case "gsm":
				RTPFormat gsm = new RTPFormat(3, FormatFactory.createAudioFormat("gsm", 8000), 8000);
				supportedFormats.add(gsm);
				break;
				
			case "g729":
				RTPFormat g729 = new RTPFormat(18, FormatFactory.createAudioFormat("g729", 8000), 8000);
				supportedFormats.add(g729);
				break;
				
			case "l16":
				RTPFormat l16 = new RTPFormat(97, FormatFactory.createAudioFormat("l16", 8000, 16, 1), 8000);
				supportedFormats.add(l16);
				break;				
				
			case "ilbc":
				RTPFormat ilbc = new RTPFormat(102, FormatFactory.createAudioFormat("ilbc", 8000, 16, 1), 8000);
				supportedFormats.add(ilbc);
				break;
				
			case "linear":
				RTPFormat linear = new RTPFormat(150, FormatFactory.createAudioFormat("linear", 8000, 16, 1), 8000);
				supportedFormats.add(linear);
				break;

			default:
				break;
			}
			
		}
		
		//Adding DTMF support
		RTPFormat dtmf = new RTPFormat(101, FormatFactory.createAudioFormat("telephone-event", 8000), 8000);
		supportedFormats.add(dtmf);
		RTPFormat dtmf126 = new RTPFormat(126, FormatFactory.createAudioFormat("telephone-event", 8000), 8000);
		supportedFormats.add(dtmf126);
		
		super.setFormats(this.supportedFormats);
	}

	public AudioComponent getAudioComponent() {
		return this.rtpChannel.getAudioComponent();
	}

	public OOBComponent getAudioOobComponent() {
		return this.rtpChannel.getOobComponent();
	}

}
