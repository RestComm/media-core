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
	
	// Default values
	private static final String DEFAULT_NAME = " ";
	
	// Error Messages
	private static final String EMPTY_TEXT = "Could not parse Session Name because text is empty";
	private static final String INVALID_TEXT = "Could not parse Session Name text: %s";
	
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
	public char getType() {
		return TYPE;
	}

	@Override
	public void parse(String text) throws SdpException {
		if(text == null || text.isEmpty()) {
			throw new SdpException(EMPTY_TEXT);
		}
		
		if(text.startsWith(BEGIN)) {
			this.name = text.substring(2);
		} else {
			throw new SdpException(String.format(INVALID_TEXT, text));
		}
		
	}
	
	@Override
	public String toString() {
		return String.format(FORMAT, this.name);
	}
	
}
