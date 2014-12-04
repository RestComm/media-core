package org.mobicents.media.server.io.sdp.attributes;

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
 * <p>
 * Although an RTP profile may make static assignments of payload type numbers
 * to payload formats, it is more common for that assignment to be done
 * dynamically using "a=rtpmap:" attributes.<br>
 * As an example of a static payload type, consider u-law PCM coded
 * single-channel audio sampled at 8 kHz. This is completely defined in the RTP
 * Audio/Video profile as payload type 0, so there is no need for an "a=rtpmap:"
 * attribute, and the media for such a stream sent to UDP port 49232 can be
 * specified as:<br>
 * <br>
 * 
 * m=audio 49232 RTP/AVP 0<br>
 * <br>
 * An example of a dynamic payload type is 16-bit linear encoded stereo audio
 * sampled at 16 kHz.<br>
 * If we wish to use the dynamic RTP/AVP payload type 98 for this stream,
 * additional information is required to decode it:<br>
 * <br>
 * m=audio 49232 RTP/AVP 98<br>
 * a=rtpmap:98 L16/16000/2
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpMapAttribute extends AttributeField {
	
	public static final String ATTRIBUTE_TYPE = "rtpmap";
	public static final short DEFAULT_CODEC_PARAMS = -1;
	
	private short payloadType;
	private String codec;
	private int clockRate;
	private short codecParams;
	
	private FormatParameterAttribute parameters;
	private PacketTimeAttribute ptime;
	private MaxPacketTimeAttribute maxptime;
	
	public RtpMapAttribute() {
		super(ATTRIBUTE_TYPE);
		this.codecParams = DEFAULT_CODEC_PARAMS;
	}
	
	public RtpMapAttribute(short payloadType, String codec, int clockRate, short codecParams) {
		super(ATTRIBUTE_TYPE);
		this.payloadType = payloadType;
		this.codec = codec;
		this.clockRate = clockRate;
		this.codecParams = codecParams;
	}

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

	public int getClockRate() {
		return clockRate;
	}

	public void setClockRate(int clockRate) {
		this.clockRate = clockRate;
	}

	public short getCodecParams() {
		return codecParams;
	}

	public void setCodecParams(short codecParams) {
		this.codecParams = codecParams;
	}

	public FormatParameterAttribute getParameters() {
		return parameters;
	}

	public void setParameters(FormatParameterAttribute parameters) {
		this.parameters = parameters;
	}

	public PacketTimeAttribute getPtime() {
		return ptime;
	}

	public void setPtime(PacketTimeAttribute ptime) {
		this.ptime = ptime;
	}

	public MaxPacketTimeAttribute getMaxptime() {
		return maxptime;
	}

	public void setMaxptime(MaxPacketTimeAttribute maxptime) {
		this.maxptime = maxptime;
	}

	@Override
	public String toString() {
		// clear builder
		super.builder.setLength(0);
		super.builder.append(BEGIN).append(ATTRIBUTE_TYPE).append(ATTRIBUTE_SEPARATOR)
		        .append(this.payloadType).append(" ")
		        .append(this.codec).append("/")
				.append(this.clockRate);
		if (this.codecParams != DEFAULT_CODEC_PARAMS) {
			super.builder.append("/").append(this.codecParams);
		}
		return builder.toString();
	}

}
