package org.mobicents.media.server.io.sdp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.attributes.FormatAttribute;
import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * Parses SDP text to construct {@link FormatAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FormatAttributeParser implements SdpParser<FormatAttribute> {

	private static final String REGEX = "^a=fmtp:\\d+(\\s\\S+)+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public FormatAttribute parse(String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			
			String[] values = sdp.substring(separator + 1).split(" ");
			short format = Short.parseShort(values[0]);
			String params = values[1];
			return new FormatAttribute(format, params);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(FormatAttribute field, String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			
			String[] values = sdp.substring(separator + 1).split(" ");
			short format = Short.parseShort(values[0]);
			String params = values[1];

			field.setFormat(format);
			field.setParams(params);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
