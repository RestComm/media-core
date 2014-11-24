package org.mobicents.media.server.io.sdp.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=ptime:[packet time]
 * 
 * <p>
 * This attribute gives the length of time in milliseconds represented by the
 * media in a packet.<br>
 * This is probably only meaningful for audio data, but may be used with other
 * media types if it makes sense. It should not be necessary to know ptime to
 * decode RTP or vat audio, and it is intended as a recommendation for the
 * encoding/packetisation of audio. It is a media-level attribute, and it is not
 * dependent on charset.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class PacketTimeAttribute extends AttributeField {
	
	private static final String NAME = "ptime";
	private static final int DEFAULT_TIME = 0;
	
	private int time;

	public PacketTimeAttribute() {
		this(DEFAULT_TIME);
	}

	public PacketTimeAttribute(int time) {
		super(NAME);
		this.time = time;
	}
	
	public void setTime(int time) {
		this.time = time;
	}
	
	public int getTime() {
		return time;
	}
	
	@Override
	public String toString() {
		super.builder.setLength(0);
		super.builder.append(BEGIN).append(NAME).append(ATTRIBUTE_SEPARATOR).append(this.time);
		return super.builder.toString();
	}

}
