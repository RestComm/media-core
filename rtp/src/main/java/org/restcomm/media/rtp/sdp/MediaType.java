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

package org.restcomm.media.rtp.sdp;

import org.restcomm.media.core.spi.utils.Text;

/**
 * Supported media types in an SDP description.
 * @author Henrique Rosa
 * 
 * @deprecated use new /io/sdp library
 */
public enum MediaType {

	AUDIO(new Text("audio")), VIDEO(new Text("video")), APPLICATION(new Text("application"));

	private Text description;

	private MediaType(Text description) {
		this.description = description;
	}
	
	public Text getDescription() {
		return description;
	}
	
	public static MediaType fromDescription(Text description) {
		for (MediaType mediaType : values()) {
			if(mediaType.getDescription().equals(description)) {
				return mediaType;
			}
		}
		return null;
	}
	
}
