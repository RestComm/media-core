package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.SdpException;

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
public class VersionField implements Field {

	private static final char TYPE = 'v';
	public static final String BEGIN = TYPE + FIELD_SEPARATOR;
	private static final int BEGIN_LENGTH = BEGIN.length();
	private static final String REGEX = "^" + BEGIN + "\\d+$";
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
		return TYPE;
	}

	public short getVersion() {
		return version;
	}

	public void setVersion(short version) {
		this.version = version;
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
			this.version = Short.parseShort(text.substring(2));
		} catch (Exception e) {
			throw new SdpException(String.format(PARSE_ERROR, text), e);
		}
	}

	@Override
	public String toString() {
		// Clean builder
		this.builder.setLength(BEGIN_LENGTH);
		this.builder.append(this.version);
		return this.builder.toString();
	}

}
