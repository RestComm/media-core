package org.mobicents.media.server.io.sdp.fields;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.exception.SdpException;

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

	private static final char TYPE = 'm';
	private static final String BEGIN = String.valueOf(TYPE) + FIELD_SEPARATOR;
	private static final String REGEX = "^" + BEGIN + "[a-zA-Z]+\\s\\d+\\s[a-zA-Z/]+(\\s\\d+)*$";

	private String media;
	private int port;
	private String protocol;
	private final List<Integer> formats;
	private List<AttributeField> attributes;

	private final StringBuilder builder = new StringBuilder();

	public MediaDescriptionField() {
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
	
	public void addFormats(int ...formats) {
		for (int format : formats) {
			this.formats.add(format);
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
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		return text.matches(REGEX);
	}

	@Override
	public void parse(String text) throws SdpException {
		if (text == null || text.isEmpty()) {
			throw new SdpException("");
		}

		if (text.matches(REGEX)) {
			String[] values = text.substring(2).split(" ");
			this.media = values[0];
			this.port = Integer.valueOf(values[1]);
			this.protocol = values[2];
			for (int i = 3; i < values.length; i++) {
				this.formats.add(Integer.valueOf(values[i]));
			}
		} else {
			throw new SdpException("");
		}

	}

	@Override
	public String toString() {
		// Clean builder
		this.builder.setLength(0);
		// Build new string
		this.builder.append(BEGIN)
		        .append(this.media).append(" ")
				.append(this.port).append(" ")
				.append(this.protocol);
		for (int payload : this.formats) {
			this.builder.append(" ").append(payload);
		}
		// TODO Print attribute fields too
		return this.builder.toString();
	}

}
