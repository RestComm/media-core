package org.mobicents.javax.media.mscontrol.networkconnection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;

public class MediaParser {

	public final static String AUDIO = "audio";
	public final static String VIDEO = "video";
	public final static String RTPMAP = "rtpmap";
	public final static String FMTP = "fmtp";

	public static Map<String, String> payloadVsFormat = new HashMap<String, String>();

	static {
		payloadVsFormat.put("0", "PCMU");
		payloadVsFormat.put("3", "GSM");
		payloadVsFormat.put("4", "G723");
		payloadVsFormat.put("5", "DVI4");
		payloadVsFormat.put("6", "DVI4");
		payloadVsFormat.put("7", "LPC");
		payloadVsFormat.put("8", "PCMA");
		payloadVsFormat.put("9", "G722");
		payloadVsFormat.put("10", "L16");
		payloadVsFormat.put("11", "L16");

		payloadVsFormat.put("12", "QCELP");
		payloadVsFormat.put("13", "CN");
		payloadVsFormat.put("14", "MPA");
		payloadVsFormat.put("15", "G728");
		payloadVsFormat.put("16", "DVI4");
		payloadVsFormat.put("17", "DVI4");
		payloadVsFormat.put("18", "G729");

	}

	public static void excludeCodec(MediaDescription md, String[] excludedCodecs) throws SdpParseException {

		Media media = md.getMedia();

		Set<String> excludeFormats = new HashSet<String>();

		for (String excludeCodec : excludedCodecs) {
			for (String standardPayload : payloadVsFormat.keySet()) {
				String standardCodec = payloadVsFormat.get(standardPayload);
				if (excludeCodec.equalsIgnoreCase(standardCodec)) {
					excludeFormats.add(standardPayload);
					break;
				}
			}
		}

		Iterator attributes = md.getAttributes(false).iterator();
		if (attributes != null) {

			// rtpmap:
			while (attributes.hasNext()) {
				Attribute a = (Attribute) attributes.next();
				for (String excludeCodec : excludedCodecs) {
					if (RTPMAP.equals(a.getName())) {
						String tokens[] = a.getValue().split(" ");
						String[] tokens1 = tokens[1].split("/");
						String encodingName = tokens1[0];

						if (encodingName.equals(excludeCodec)) {
							excludeFormats.add(tokens[0]);
							attributes.remove();
						}

					}
				}
			}

			// fmtp:
			attributes = md.getAttributes(false).iterator();
			while (attributes.hasNext()) {
				Attribute a = (Attribute) attributes.next();
				for (String f : excludeFormats) {					
					if (FMTP.equals(a.getName())) {
						String tokens[] = a.getValue().split(" ");
						if (tokens[0].equals(f)) {
							attributes.remove();
						}
					}
				}
			}

		}

		Vector formats = media.getMediaFormats(false);
		formats.removeAll(excludeFormats);
	}
	
	public static void RequiredCodec(MediaDescription md, String[] excludedCodecs) throws SdpParseException {
		
	}
}
