package org.mobicents.media.server.io.sdp.dtls.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;

/**
 * Parses SDP text to construct {@link SetupAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SetupAttributeParser implements SdpParser<SetupAttribute> {

	private static final String REGEX = "^a=setup:(active|passive|actpass|holdconn)$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public SetupAttribute parse(String sdp) throws SdpException {
		try {			
			return new SetupAttribute(sdp.substring(8));
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(SetupAttribute field, String sdp) throws SdpException {
		try {
			field.setValue(sdp.substring(8));
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}		
	}

}
