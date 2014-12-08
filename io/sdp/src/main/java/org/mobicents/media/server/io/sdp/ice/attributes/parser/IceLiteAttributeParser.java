package org.mobicents.media.server.io.sdp.ice.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.ice.attributes.IceLiteAttribute;

/**
 * Parses SDP text to construct {@link IceLiteAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class IceLiteAttributeParser implements SdpParser<IceLiteAttribute> {

	private static final String REGEX = "^a=ice-lite$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public IceLiteAttribute parse(String sdp) throws SdpException {
		return new IceLiteAttribute();
	}

	@Override
	public void parse(IceLiteAttribute field, String sdp) throws SdpException {
		// nothing to do
	}

}
