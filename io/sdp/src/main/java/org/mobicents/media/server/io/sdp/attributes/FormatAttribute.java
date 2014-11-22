package org.mobicents.media.server.io.sdp.attributes;

import org.mobicents.media.server.io.sdp.AttributeField;

/**
 * a=fmtp:[format] [format specific parameters]<br>
 * 
 * <p>
 * This attribute allows parameters that are specific to a particular format to
 * be conveyed in a way that SDP does not have to understand them.<br>
 * The format must be one of the formats specified for the media.
 * Format-specific parameters may be any set of parameters required to be
 * conveyed by SDP and given unchanged to the media tool that will use this
 * format. At most one instance of this attribute is allowed for each format.<br>
 * It is a media-level attribute, and it is not dependent on charset.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FormatAttribute extends AttributeField {
	
	private static final String NAME = "fmtp";
	private static final String REGEX = "^" + BEGIN + NAME + ATTRIBUTE_SEPARATOR + "\\d+(\\s\\S+)+$";
	
	public FormatAttribute() {
		super(true);
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
