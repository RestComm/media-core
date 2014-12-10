package org.mobicents.media.server.io.sdp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.attributes.MaxPacketTimeAttribute;
import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * Parses SDP text to construct {@link MaxPacketTimeAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MaxPacketTimeAttributeParser implements SdpParser<MaxPacketTimeAttribute> {

	private static final String REGEX = "^a=maxptime:\\d+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public MaxPacketTimeAttribute parse(String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			int value = Integer.parseInt(sdp.trim().substring(separator + 1));
			return new MaxPacketTimeAttribute(value);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(MaxPacketTimeAttribute field, String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			int value = Integer.parseInt(sdp.trim().substring(separator + 1));
			field.setTime(value);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
