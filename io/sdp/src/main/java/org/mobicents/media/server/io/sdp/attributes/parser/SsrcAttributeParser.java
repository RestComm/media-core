package org.mobicents.media.server.io.sdp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;

/**
 * Parses SDP text to construct {@link SsrcAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SsrcAttributeParser implements SdpParser<SsrcAttribute> {

	private static final String REGEX = "^a=ssrc:\\S+\\s\\w+(:\\S+)?$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	private static final String COLON = ":";
	private static final String WHITESPACE = " ";

	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public SsrcAttribute parse(String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(7).split(WHITESPACE);
			int separator = values[1].indexOf(COLON);

			String attName = (separator == -1) ? values[1] : values[1].substring(0, separator);
			String attValue = (separator == -1) ? null : values[1].substring(separator + 1);
			
			SsrcAttribute ssrc = new SsrcAttribute(values[0]);
			ssrc.addAttribute(attName, attValue);
			return ssrc;
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(SsrcAttribute field, String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(7).split(WHITESPACE);
			int separator = values[1].indexOf(COLON);

			String attName = (separator == -1) ? values[1] : values[1].substring(0, separator);
			String attValue = (separator == -1) ? null : values[1].substring(separator + 1);
			
			// If ssrc-id is different then it means we are processing new sdp
			// Reset original ssrc and start writing new ssrc information
			if(!field.getSsrcId().equals(values[0])) {
				field.reset();
				field.setSsrcId(values[0]);
			}
			
			// Keep adding attributes to the same ssrc
			field.addAttribute(attName, attValue);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR, e);
		}
	}

}
