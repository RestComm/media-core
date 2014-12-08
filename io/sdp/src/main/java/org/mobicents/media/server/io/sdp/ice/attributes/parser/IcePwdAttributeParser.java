package org.mobicents.media.server.io.sdp.ice.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.AttributeField;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;

/**
 * Parses SDP text to construct {@link IcePwdAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class IcePwdAttributeParser implements SdpParser<IcePwdAttribute> {

	private static final String REGEX = "^a=ice-pwd\\S+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public IcePwdAttribute parse(String sdp) throws SdpException {
		try {
			// extract data from sdp
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("Attribute has no value");
			}
			
			String password = sdp.trim().substring(separator + 1);
			if(password.isEmpty()) {
				throw new IllegalArgumentException("Value is empty");
			}
			
			// Build object
			return new IcePwdAttribute(password);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(IcePwdAttribute field, String sdp) throws SdpException {
		try {
			// extract data from sdp
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("Attribute has no value");
			}
			
			String password = sdp.trim().substring(separator + 1);
			if(password.isEmpty()) {
				throw new IllegalArgumentException("Value is empty");
			}
			
			// Build object
			field.setPassword(password);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
