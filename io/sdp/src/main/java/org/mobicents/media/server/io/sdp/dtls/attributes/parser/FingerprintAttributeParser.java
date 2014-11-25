package org.mobicents.media.server.io.sdp.dtls.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * Parses SDP text to construct {@link FingerprintAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FingerprintAttributeParser implements SdpParser<FingerprintAttribute> {

	private static final String REGEX = "^a=fingerprint:\\S+\\s(\\w+(:\\w+)+)$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public FingerprintAttribute parse(String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			
			String[] values = sdp.substring(separator + 1).split(" ");
			String hashFunction = values[0];
			String fingerprint = values[1];
			return new FingerprintAttribute(hashFunction, fingerprint);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(FingerprintAttribute field, String sdp) throws SdpException {
		try {
			int separator = sdp.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
			if(separator == -1) {
				throw new IllegalArgumentException("No value found");
			}
			
			String[] values = sdp.substring(separator + 1).split(" ");
			String hashFunction = values[0];
			String fingerprint = values[1];
			field.setHashFunction(hashFunction);
			field.setFingerprint(fingerprint);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
