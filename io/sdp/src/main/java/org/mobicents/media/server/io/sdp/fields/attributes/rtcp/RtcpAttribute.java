package org.mobicents.media.server.io.sdp.fields.attributes.rtcp;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=rtcp:[port]
 * 
 * <p>
 * The RTCP attribute is used to document the RTCP port used for media stream,
 * when that port is not the next higher (odd) port number following the RTP
 * port described in the media line.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see <a href="https://tools.ietf.org/html/rfc3605">RFC3605</a>
 */
public class RtcpAttribute extends AttributeField {
	
	private static final String NAME = "rtcp";
	private static final String REGEX = "^" + BEGIN + NAME + ATTRIBUTE_SEPARATOR + "\\d+$";
	
	public RtcpAttribute() {
		this.key = NAME;
	}
	
	public void setValue(int value) {
		this.value = String.valueOf(value);
	}
	
	@Override
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		return text.matches(REGEX);
	}
	
	@Override
	protected boolean isComplex() {
		return true;
	}

}
