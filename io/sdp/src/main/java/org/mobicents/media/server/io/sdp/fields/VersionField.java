package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.exception.SdpException;

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
	private static final String INITIALIZATION = TYPE + "=";
	private static final String TEMPLATE = INITIALIZATION + "%d";
	private static final short DEFAULT_VERSION = 0;

	// Error Messages
	private static final String EMPTY_TEXT = "Could not parse Protocol Version because text is empty";
	private static final String INVALID_TEXT = "Could not parse Protocol Version text: %s";

	private short version;

	public VersionField() {
		this(DEFAULT_VERSION);
	}

	public VersionField(short version) {
		this.version = version;
	}

	public VersionField(String text) throws SdpException {
		parse(text);
	}

	@Override
	public char getType() {
		return TYPE;
	}

	public short getVersion() {
		return version;
	}

	public void setVersion(short version) {
		this.version = version;
	}

	@Override
	public void parse(String text) throws SdpException {
		if (text == null || text.isEmpty()) {
			throw new SdpException(EMPTY_TEXT);
		}

		if (text.startsWith(INITIALIZATION)) {
			try {
				this.version = Short.parseShort(text.substring(2));
			} catch (NumberFormatException e) {
				throw new SdpException(String.format(INVALID_TEXT, text));
			}
		} else {
			throw new SdpException(String.format(INVALID_TEXT, text));
		}
	}

	@Override
	public String toString() {
		return String.format(TEMPLATE, this.version);
	}

}
