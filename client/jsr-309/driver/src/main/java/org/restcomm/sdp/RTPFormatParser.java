/*
 * RTPFormat.java
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
import java.util.Enumeration;
import java.util.HashMap;

import javax.sdp.Attribute;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;



/**
 * 
 * @author Oleg Kulikov
 */
public abstract class RTPFormatParser extends Format {

	private int payloadType;

	/** Creates a new instance of RTPFormat */
	public RTPFormatParser(String encodingName) {
		super(encodingName);
	}

	public int getPayloadType() {
		return payloadType;
	}

	public static synchronized HashMap<Integer, Format> getFormats(SessionDescription sdp, String mediaType)
			throws SdpParseException, SdpException {
		HashMap<Integer, Format> formats = new HashMap<Integer, Format>();
		Enumeration mediaDescriptions = sdp.getMediaDescriptions(false).elements();
		while (mediaDescriptions.hasMoreElements()) {
			MediaDescription md = (MediaDescription) mediaDescriptions.nextElement();
			if (md.getMedia().getMediaType().equals(mediaType)) {
				HashMap<Integer, Format> fmts = getFormats(md);
				if (fmts != null) {
					formats.putAll(fmts);
				}
			}
		}
		return formats;
	}

	public static HashMap<Integer, Format> getFormats(MediaDescription md) throws SdpParseException, SdpException {
		Media media = md.getMedia();
		if (media.getMediaType().equals(AVProfile.AUDIO)) {
			return getAudioFormats(md);
		} else if (media.getMediaType().equals(AVProfile.VIDEO)) {
			return getVideoFormats(md);
		} else {
			return null;
		}
	}

	private static HashMap<Integer, Format> getAudioFormats(MediaDescription md) throws SdpParseException, SdpException {
		HashMap<Integer, Format> formats = new HashMap<Integer, Format>();
		Media media = md.getMedia();

		Enumeration payloads = media.getMediaFormats(false).elements();
		while (payloads.hasMoreElements()) {
			int payload = Integer.parseInt((String) payloads.nextElement());
			Format fmt = AVProfile.getAudioFormat(payload);
			if (fmt != null) {
				formats.put(new Integer(payload), fmt);
			}
		}

		Enumeration attributes = md.getAttributes(false).elements();
		while (attributes.hasMoreElements()) {
			Attribute attribute = (Attribute) attributes.nextElement();
			if (attribute.getName().equals("rtpmap")) {
				RTPAudioFormat fmt = RTPAudioFormat.parseRtpmapFormat(attribute.getValue());
				if (fmt != null) {
					formats.put(new Integer(fmt.getPayloadType()), fmt);
				}
			}
		}

		return formats;
	}

	private static HashMap<Integer, Format> getVideoFormats(MediaDescription md) throws SdpParseException, SdpException {
		HashMap<Integer, Format> formats = new HashMap<Integer, Format>();
		Media media = md.getMedia();

		Enumeration<String> payloads = media.getMediaFormats(false).elements();
		while (payloads.hasMoreElements()) {
			int payload = Integer.parseInt((String) payloads.nextElement());
			Format fmt = AVProfile.getVideoFormat(payload);
			if (fmt != null) {
				formats.put(new Integer(payload), fmt);
			}
		}

		Enumeration attributes = md.getAttributes(false).elements();
		while (attributes.hasMoreElements()) {
			Attribute attribute = (Attribute) attributes.nextElement();
			if (attribute.getName().equals("rtpmap")) {
				RTPVideoFormat fmt = RTPVideoFormat.parseFormat(attribute.getValue());
				formats.put(new Integer(fmt.getPayloadType()), fmt);
			}
		}

		return formats;
	}

	public String toSdp() {
		return null;
	}

	public abstract Collection<Attribute> encode();
}
