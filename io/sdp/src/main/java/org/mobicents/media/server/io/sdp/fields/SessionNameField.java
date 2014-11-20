package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.exception.SdpException;

/**
 * s=[session name]
 * <p>
 * The "s=" field is the textual session name.<br>
 * There MUST be one and only one "s=" field per session description.<br>
 * The "s=" field MUST NOT be empty. If a session has no meaningful name, the
 * value "s= " SHOULD be used (i.e., a single space as the session name).
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SessionNameField implements Field {

	private static final char TYPE = 's';
	private static final String BEGIN = TYPE + "=";
	private static final String FORMAT = BEGIN + "%s";
	private static final String REGEX = "^" + BEGIN + "\\s|(\\S+\\s?)+$";
	
	// Default values
	private static final String DEFAULT_NAME = " ";
	
	private String name;
	
	public SessionNameField(String name) {
		this.name = name;
	}
	
	public SessionNameField() {
		this(DEFAULT_NAME);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
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
		try {
			this.name = text.substring(2);
		} catch (Exception e) {
			throw new SdpException(String.format(PARSE_ERROR, text), e);
		}
	}
	
	@Override
	public String toString() {
		return String.format(FORMAT, this.name);
	}
	
}
