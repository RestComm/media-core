package org.mobicents.media.server.io.sdp.ice.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.AttributeField;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;

public class IceUfragAttributeParser implements SdpParser<IceUfragAttribute> {

	private static final String REGEX = "^a=ice-ufrag:\\S+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public IceUfragAttribute parse(String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalAccessException("No value found");
			}
			
			String ufrag = sdp.trim().substring(separator + 1);
			if(ufrag.isEmpty()) {
				throw new IllegalArgumentException("No value found");
			}
			return new IceUfragAttribute(ufrag);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(IceUfragAttribute field, String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalAccessException("No value found");
			}
			
			String ufrag = sdp.trim().substring(separator + 1);
			if(ufrag.isEmpty()) {
				throw new IllegalArgumentException("No value found");
			}
			field.setUfrag(ufrag);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}
	
}
