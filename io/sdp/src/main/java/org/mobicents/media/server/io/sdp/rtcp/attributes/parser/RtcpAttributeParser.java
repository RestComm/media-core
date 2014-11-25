package org.mobicents.media.server.io.sdp.rtcp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.AttributeField;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;

/**
 * Parses SDP text to construct {@link RtcpAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpAttributeParser implements SdpParser<RtcpAttribute> {

	private static final String REGEX = "^a=rtcp:\\d+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}
	
	@Override
	public RtcpAttribute parse(String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			
			int port = Integer.parseInt(sdp.substring(separator + 1));
			return new RtcpAttribute(port);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}
	
	@Override
	public void parse(RtcpAttribute field, String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			
			int port = Integer.parseInt(sdp.substring(separator + 1));
			field.setPort(port);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}
	
}
