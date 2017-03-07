/*
 * RTPAudioFormat.java
 *
 * Mobicents Media Gateway
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */
package org.restcomm.sdp;

import java.util.Collection;
import java.util.Vector;
import javax.sdp.Attribute;
import javax.sdp.SdpFactory;

/**
 * This represents the RTP Audio Format. The difference being RTPAudioFormat also carries the pay-load. For example look
 * at SDP <br/>
 * <p>
 * <blockquote>
 * 
 * <pre>
 * m=audio 8010 RTP/AVP 0 8 97 
 * a=rtpmap:0 PCMU/8000 
 * a=rtpmap:8 PCMA/8000
 * a=ptime:20 	
 * </pre>
 * 
 * </blockquote>
 * </p>
 * 
 * <br/>
 * 
 * here pay-load is 0 for PCMU, 8 for PCMA
 * 
 */
public class RTPAudioFormat extends AudioFormat implements RTPFormat {

	private int payloadType;
	private final static SdpFactory sdpFactory = SdpFactory.getInstance();

	/** Creates a new instance of RTPAudioFormat */
	public RTPAudioFormat(int payload, String encodingName) {
		super(encodingName);
		this.payloadType = payload;
	}

	public RTPAudioFormat(int payload, String encodingName, double sampleRate, int bits, int chans) {
		super(encodingName, sampleRate, bits, chans);
		this.payloadType = payload;
	}

	public RTPAudioFormat(int payload, String encodingName, double sampleRate, int bits, int chans, int endian,
			int signed) {
		super(encodingName, sampleRate, bits, chans, endian, signed);
		this.payloadType = payload;
	}

	public int getPayloadType() {
		return payloadType;
	}

	public void setPayloadType(int payload) {
		this.payloadType = payload;
	}

	public static RTPAudioFormat parseFormat(String encodingName) {

		encodingName = encodingName.toLowerCase();

		if (encodingName.equals("pcmu")) {
			return AVProfile.PCMU;
		} else if (encodingName.equals("g723")) {
			return AVProfile.G723;
		} else if (encodingName.equals("pcma")) {
			return AVProfile.PCMA;
		} else if (encodingName.equals("telephone-event")) {
			return AVProfile.DTMF;
		} else if (encodingName.equals("g729")) {
			return AVProfile.G729;
		} else if (encodingName.equals("gsm")) {
			return AVProfile.GSM;
		} else if (encodingName.equals("l16")) {
			return AVProfile.L16_MONO;
		}

		return null;

	}

	public static RTPAudioFormat parseRtpmapFormat(String rtpmap) {
		String tokens[] = rtpmap.toLowerCase().split(" ");

		// split params
		int p = Integer.parseInt(tokens[0]);
		tokens = tokens[1].split("/");

		String encodingName = tokens[0];
		double clockRate = Double.parseDouble(tokens[1]);

		int chans = 1;
		if (tokens.length == 3) {
			chans = Integer.parseInt(tokens[2]);
		}

		if (encodingName.equals("pcmu")) {
			return new RTPAudioFormat(p, AudioFormat.ULAW, clockRate, 8, chans);
		} else if (encodingName.equals("g723")) {
			return new RTPAudioFormat(p, AudioFormat.G723, clockRate, AudioFormat.NOT_SPECIFIED, chans);
		} else if (encodingName.equals("pcma")) {
			return new RTPAudioFormat(p, AudioFormat.ALAW, clockRate, 8, chans);
		} else if (encodingName.equals("telephone-event")) {
			return new RTPAudioFormat(p, "telephone-event", clockRate, AudioFormat.NOT_SPECIFIED,
					AudioFormat.NOT_SPECIFIED);
		} else if (encodingName.equals("g729")) {
			return new RTPAudioFormat(p, AudioFormat.G729, clockRate, AudioFormat.NOT_SPECIFIED, chans);
		} else if (encodingName.equals("gsm")) {
			return new RTPAudioFormat(p, AudioFormat.GSM, clockRate, AudioFormat.NOT_SPECIFIED, chans);
		} else if (encodingName.equals("l16")) {
			return new RTPAudioFormat(p, AudioFormat.LINEAR, clockRate, 16, chans, AudioFormat.LITTLE_ENDIAN,
					AudioFormat.SIGNED);
		} else {
			return new RTPAudioFormat(p, encodingName, clockRate, AudioFormat.NOT_SPECIFIED, chans);
		}
	}

	public Collection<Attribute> encode() {
		Vector<Attribute> list = new Vector();
		list.add(sdpFactory.createAttribute("rtpmap", toSdp()));
		if (getEncoding().equalsIgnoreCase("telephone-event")) {
			list.add(sdpFactory.createAttribute("fmtp", payloadType + " 0-15"));
		} else if (getEncoding().equalsIgnoreCase("g729")) {
			list.add(sdpFactory.createAttribute("fmtp", payloadType + " annex=b"));
		}
		return list;
	}

	public String toSdp() {
		String encName = this.getEncoding().toLowerCase();
		StringBuffer buff = new StringBuffer();
		buff.append(payloadType);
		buff.append(" ");

		if (encName.equals("alaw")) {
			buff.append("pcma");
		} else if (encName.equals("ulaw")) {
			buff.append("pcmu");
		} else if (encName.equals("linear")) {
			buff.append("l" + this.sampleSizeInBits);
		} else {
			buff.append(encName);
		}

		double sr = getSampleRate();
		if (sr > 0) {
			buff.append("/");

			if ((sr - (int) sr) < 1E-6) {
				buff.append((int) sr);
			} else {
				buff.append(sr);
			}
		}
		if (getChannels() > 1) {
			buff.append("/" + getChannels());
		}

		return buff.toString();
	}
}
