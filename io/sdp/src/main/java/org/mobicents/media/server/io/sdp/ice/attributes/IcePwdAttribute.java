package org.mobicents.media.server.io.sdp.ice.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=ice-pwd:[value]
 * 
 * <p>
 * The "ice-pwd" and "ice-ufrag" attributes can appear at either the
 * session-level or media-level. When present in both, the value in the
 * media-level takes precedence. Thus, the value at the session-level is
 * effectively a default that applies to all media streams, unless overridden by
 * a media-level value. Whether present at the session or media-level, there
 * MUST be an ice-pwd and ice-ufrag attribute for each media stream. If two
 * media streams have identical ice-ufrag's, they MUST have identical ice-pwd's.
 * 
 * The ice-ufrag and ice-pwd attributes MUST be chosen randomly at the beginning
 * of a session. The ice-ufrag attribute MUST contain at least 24 bits of
 * randomness, and the ice-pwd attribute MUST contain at least 128 bits of
 * randomness. This means that the ice-ufrag attribute will be at least 4
 * characters long, and the ice-pwd at least 22 characters long, since the
 * grammar for these attributes allows for 6 bits of randomness per character.
 * The attributes MAY be longer than 4 and 22 characters, respectively, of
 * course, up to 256 characters. The upper limit allows for buffer sizing in
 * implementations. Its large upper limit allows for increased amounts of
 * randomness to be added over time.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see <a href="https://tools.ietf.org/html/rfc5245#section-15.4">RFC5245</a>
 */
public class IcePwdAttribute extends AttributeField {
	
	private static final String NAME = "ice-pwd";
	private static final String REGEX = "^" + BEGIN + NAME + ATTRIBUTE_SEPARATOR +"\\S+$";
	
	public IcePwdAttribute() {
		super(true);
		this.key = NAME;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		return text.matches(REGEX);
	}

}
