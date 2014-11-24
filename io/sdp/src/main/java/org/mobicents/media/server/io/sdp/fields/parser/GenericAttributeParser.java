package org.mobicents.media.server.io.sdp.fields.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.attributes.GenericAttribute;
import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * Parses SDP text to construct {@link GenericAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class GenericAttributeParser implements SdpParser<GenericAttribute> {

	protected static final String REGEX = "^a=\\S+(:\\S+\\s?)?$";
	protected static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public GenericAttribute parse(String sdp) throws SdpException {
		try {
			// Extract data from SDP
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			boolean hasValue = (separator != -1);
			
			String key;
			String value;

			if(!hasValue) {
				key = sdp.substring(2);
				value = null;
			} else {
				key = sdp.substring(2, separator);
				value = sdp.substring(separator + 1);
			}
			
			// Build object
			return new GenericAttribute(key, value);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(GenericAttribute field, String sdp) throws SdpException {
		try {
			// Extract data from SDP
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			boolean hasValue = (separator != -1);
			
			String key;
			String value;

			if(!hasValue) {
				key = sdp.substring(2);
				value = null;
			} else {
				key = sdp.substring(2, separator);
				value = sdp.substring(separator + 1);
			}
			
			if(key.isEmpty()) {
				throw new IllegalArgumentException("The key is empty");
			}
			
			// Build object
			field.setKey(key);
			field.setValue(value);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
