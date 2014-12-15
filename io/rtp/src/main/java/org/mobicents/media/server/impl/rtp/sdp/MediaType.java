package org.mobicents.media.server.impl.rtp.sdp;

import org.mobicents.media.server.utils.Text;

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
