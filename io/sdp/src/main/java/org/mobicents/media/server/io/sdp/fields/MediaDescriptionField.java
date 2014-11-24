package org.mobicents.media.server.io.sdp.fields;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.media.server.io.sdp.AttributeField;
import org.mobicents.media.server.io.sdp.Field;

/**
 * m=[media] [port] [proto] [fmt]
 * 
 * <p>
 * A session description may contain a number of media descriptions.<br>
 * Each media description starts with an "m=" field and is terminated by either
 * the next "m=" field or by the end of the session description.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MediaDescriptionField implements Field {
	
	private static final String PROFILE_RTP_AVP = "RTP/AVP";
	private static final String PROFILE_RTP_AVPF = "RTP/AVPF";
	private static final String PROFILE_RTP_SAVP = "RTP/SAVP";
	private static final String PROFILE_RTP_SAVPF = "RTP/SAVPF";
	
	private static final char TYPE = 'm';
	private static final String BEGIN = TYPE + FIELD_SEPARATOR;
	private static final int BEGIN_LENGTH = BEGIN.length();

	private String media;
	private int port;
	private String protocol;
	private final List<Integer> formats;
	private List<AttributeField> attributes;

	private final StringBuilder builder;

	public MediaDescriptionField() {
		this.builder = new StringBuilder(BEGIN);
		this.formats = new ArrayList<Integer>(15);
		this.attributes = new ArrayList<AttributeField>(20);
	}
	
	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public void setFormats(int ...formats) {
		this.formats.clear();
		int numFormats = formats.length;
		for (int i = 0; i < numFormats; i++) {
			this.formats.add(formats[i]);
		}
	}
	
	public void addFormats(int ...formats) {
		int numFormats = formats.length;
		for (int i = 0; i < numFormats; i++) {
			this.formats.add(formats[i]);
		}
	}
	
	public boolean containsFormat(int format) {
		for (int fmt : this.formats) {
			if(format == fmt) {
				return true;
			}
		}
		return false;
	}

	@Override
	public char getFieldType() {
		return TYPE;
	}

	@Override
	public String toString() {
		// Clean builder
		this.builder.setLength(BEGIN_LENGTH);
		this.builder.append(this.media).append(" ")
				.append(this.port).append(" ")
				.append(this.protocol);
		for (int payload : this.formats) {
			this.builder.append(" ").append(payload);
		}
		// Append new lines for attributes
		for (AttributeField attribute : this.attributes) {
			this.builder.append("\n").append(attribute.toString());
		}
		return this.builder.toString();
	}
	
	public static boolean isValidProfile(String profile) {
		if(profile == null || profile.isEmpty()) {
			return false;
		}
		return PROFILE_RTP_AVP.equals(profile)
				|| PROFILE_RTP_AVPF.equals(profile)
				|| PROFILE_RTP_SAVP.equals(profile)
				|| PROFILE_RTP_SAVPF.equals(profile);
	}

}
