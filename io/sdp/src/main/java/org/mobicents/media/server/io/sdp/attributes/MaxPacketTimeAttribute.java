package org.mobicents.media.server.io.sdp.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=maxptime:[maximum packet time]
 * 
 * <p>
 * This gives the maximum amount of media that can be encapsulated in each
 * packet, expressed as time in milliseconds.<br>
 * The time SHALL be calculated as the sum of the time the media present in the
 * packet represents. For frame-based codecs, the time SHOULD be an integer
 * multiple of the frame size. This attribute is probably only meaningful for
 * audio data, but may be used with other media types if it makes sense. It is a
 * media-level attribute, and it is not dependent on charset.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MaxPacketTimeAttribute extends AttributeField {
	
	private static final String NAME = "maxptime";
	private static final int DEFAULT_TIME = 0;

	private int time;
	
	public MaxPacketTimeAttribute() {
		this(DEFAULT_TIME);
	}
	
	public MaxPacketTimeAttribute(int time) {
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