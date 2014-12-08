package org.mobicents.media.server.io.sdp.rtcp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;

/**
 * Parses SDP text to construct {@link RtcpMuxAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpMuxAttributeParser implements SdpParser<RtcpMuxAttribute> {

	private static final String REGEX = "^a=rtcp-mux$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public RtcpMuxAttribute parse(String sdp) throws SdpException {
		return new RtcpMuxAttribute();
	}

	@Override
	public void parse(RtcpMuxAttribute field, String sdp) throws SdpException {
		// nothing to do
	}

}
