package org.mobicents.media.server.io.sdp.ice.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=ice-lite
 * 
 * <p>
 * Session-level attribute only, and indicates that an agent is a lite
 * implementation.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see <a href="https://tools.ietf.org/html/rfc5245#section-15.3">RFC5245</a>
 */
public class IceLiteAttribute extends AttributeField {

	private static final String NAME = "ice-lite";
	private static final String REGEX = "^" + BEGIN + "(" + NAME + ")$";

	public IceLiteAttribute() {
		super(false);
		this.key = NAME;
	}
	
	@Override
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		return text.matches(REGEX);
	}

}
