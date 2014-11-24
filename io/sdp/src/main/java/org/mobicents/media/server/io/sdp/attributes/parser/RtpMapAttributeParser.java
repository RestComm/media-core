package org.mobicents.media.server.io.sdp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;

/**
 * Parses SDP text to construct {@link RtpMapAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpMapAttributeParser implements SdpParser<RtpMapAttribute> {

	private static final String REGEX = "^a=rtpmap:\\d+\\s\\w+/\\d+(/\\d+)?$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public RtpMapAttribute parse(String sdp) throws SdpException {
		try {
			// Extract data from SDP
			int index = 0;
			String[] values = sdp.substring(9).split("\\s|/");
			
			short payloadType = Short.parseShort(values[index++]);
			String codec = values[index++];
			short clockRate = Short.parseShort(values[index++]);
			short codecParams = -1;
			if(index == values.length - 1) {
				codecParams = Short.parseShort(values[index]);
			}
			
			// Build object from extracted data
			return new RtpMapAttribute(payloadType, codec, clockRate, codecParams);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(RtpMapAttribute field, String sdp) throws SdpException {
		try {
			// Extract data from SDP
			int index = 0;
			String[] values = sdp.substring(9).split("\\s|/");
			
			short payloadType = Short.parseShort(values[index++]);
			String codec = values[index++];
			short clockRate = Short.parseShort(values[index++]);
			short codecParams = -1;
			if(index == values.length - 1) {
				codecParams = Short.parseShort(values[index]);
			}
			
			// Build object from extracted data
			field.setPayloadType(payloadType);
			field.setCodec(codec);
			field.setClockRate(clockRate);
			field.setCodecParams(codecParams);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}
}
