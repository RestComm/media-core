package org.mobicents.media.server.io.sdp.fields.attributes;

import org.mobicents.media.server.io.sdp.exception.SdpException;
import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=rtpmap:[payload type][encoding name]/[clock rate]/[encoding parameters*]<br>
 * 
 * <p>
 * m=audio 49230 RTP/AVP 96 97 98<br>
 * a=rtpmap:96 L8/8000<br>
 * a=rtpmap:97 L16/8000<br>
 * a=rtpmap:98 L16/11025/2
 * </p>
 * 
 * <p>
 * This attribute maps from an RTP payload type number (as used in an "m=" line)
 * to an encoding name denoting the payload format to be used.<br>
 * It also provides information on the clock rate and encoding parameters. It is
 * a media-level attribute that is not dependent on charset.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpMapAttribute extends AttributeField {
	
	private static final String NAME = "rtpmap";
	private static final String TO_ATTR_SEPARATOR = BEGIN + NAME + ATTRIBUTE_SEPARATOR;
	private static final int TO_ATTR_SEPARATOR_LENGTH = TO_ATTR_SEPARATOR.length();
	private static final String REGEX = "^" + TO_ATTR_SEPARATOR + "\\d+\\s\\w+/\\d+(/\\d+)?$";
	
	private static final String SIMPLE_FORMAT = TO_ATTR_SEPARATOR + "%d %d/%d";
	private static final String COMPLEX_FORMAT = SIMPLE_FORMAT + "/%d";
	
	private short payloadType;
	private String codec;
	private short clockRate;
	private short codecParams;
	
	public short getPayloadType() {
		return payloadType;
	}

	public void setPayloadType(short payloadType) {
		this.payloadType = payloadType;
	}

	public String getCodec() {
		return codec;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public short getClockRate() {
		return clockRate;
	}

	public void setClockRate(short clockRate) {
		this.clockRate = clockRate;
	}

	public short getCodecParams() {
		return codecParams;
	}

	public void setCodecParams(short codecParams) {
		this.codecParams = codecParams;
	}
	
	@Override
	protected boolean isComplex() {
		return true;
	}
	
	@Override
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		return text.matches(REGEX);
	}
	
	@Override
	public void parse(String text) throws SdpException {
		try {
			int index = 0;
			String[] values = text.substring(TO_ATTR_SEPARATOR_LENGTH).split("\\s|/");
			
			this.payloadType = Short.valueOf(values[index++]);
			this.codec = values[index++];
			this.clockRate = Short.valueOf(values[index++]);
			if(index == values.length - 1) {
				this.codecParams = Short.valueOf(values[index]);
			} else {
				this.codecParams = -1;
			}
		} catch (Exception e) {
			throw new SdpException(String.format(PARSE_ERROR, text), e);
		}
	}

	@Override
	public String toString() {
		if(this.codecParams == -1) {
			return String.format(SIMPLE_FORMAT, this.payloadType, this.codec, this.clockRate);
		} 
		return String.format(COMPLEX_FORMAT, this.payloadType, this.codec, this.clockRate, this.codecParams);
	}

}
