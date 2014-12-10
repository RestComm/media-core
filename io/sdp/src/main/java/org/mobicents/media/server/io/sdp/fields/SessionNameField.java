package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.SdpField;

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
public class SessionNameField implements SdpField {

	public static final char FIELD_TYPE = 's';
	private static final String BEGIN = "s=";
	private static final int BEGIN_LEN = BEGIN.length();
	
	// Default values
	private static final String DEFAULT_NAME = " ";
	
	private final StringBuilder builder;
	
	private String name;
	
	public SessionNameField(String name) {
		this.builder = new StringBuilder(BEGIN);
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
		return FIELD_TYPE;
	}

	@Override
	public String toString() {
		// Clear builder
		this.builder.setLength(BEGIN_LEN);
		this.builder.append(this.name);
		return this.builder.toString();
	}
	
}
