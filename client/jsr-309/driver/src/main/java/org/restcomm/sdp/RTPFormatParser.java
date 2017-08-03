/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
 *  
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *  
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
