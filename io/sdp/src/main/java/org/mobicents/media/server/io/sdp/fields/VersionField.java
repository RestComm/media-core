package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.SdpField;

/**
 * v=[version number]
 * <p>
 * The "v=" field gives the version of the Session Description Protocol.<br>
 * This implementation defines version 0 as written on rfc4566. There is no
 * minor version number.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class VersionField implements SdpField {

	public static final char FIELD_TYPE = 'v';
	public static final String BEGIN = "v=";
	private static final int BEGIN_LENGTH = BEGIN.length();

	private static final short DEFAULT_VERSION = 0;
	
	private final StringBuilder builder;

	private short version;

	public VersionField() {
		this(DEFAULT_VERSION);
	}

	public VersionField(short version) {
		this.builder = new StringBuilder(BEGIN);
		this.version = version;
	}

	@Override
	public char getFieldType() {
		return FIELD_TYPE;
	}

	public short getVersion() {
		return version;
	}

	public void setVersion(short version) {
		this.version = version;
	}

	@Override
	public String toString() {
		// Clean builder
		this.builder.setLength(BEGIN_LENGTH);
		this.builder.append(this.version);
		return this.builder.toString();
	}

}
