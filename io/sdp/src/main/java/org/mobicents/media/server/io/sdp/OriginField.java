package org.mobicents.media.server.io.sdp;

import org.mobicents.media.server.io.sdp.exception.SdpException;

/**
 * o=[username] [sess-id] [sess-version] [nettype] [addrtype] [unicast-address]
 * <p>
 * The "o=" field gives the originator of the session (her username and the
 * address of the user's host) plus a session identifier and version number.
 * </p>
 * <p>
 * In general, the "o=" field serves as a globally unique identifier for this
 * version of this session description, and the subfields excepting the version
 * taken together identify the session irrespective of any modifications.
 * </p>
 * <p>
 * For privacy reasons, it is sometimes desirable to obfuscate the username and
 * IP address of the session originator. If this is a concern, an arbitrary
 * <username> and private <unicast-address> MAY be chosen to populate the "o="
 * field, provided that these are selected in a manner that does not affect the
 * global uniqueness of the field.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class OriginField implements Field {

	// Parsing
	private static final char TYPE = 'o';
	private static final String BEGIN = TYPE + "=";
	private static final String FORMAT = BEGIN + "%s %d %d %s %s %s";
	
	// Default values
	private static final String DEFAULT_USERNAME = "-";
	private static final int DEFAULT_SESSION_ID = 0;
	private static final int DEFAULT_SESSION_VERSION = 1;
	private static final String DEFAULT_NET_TYPE = "IN";
	private static final String DEFAULT_ADDRESS_TYPE = "IP4";
	private static final String DEFAULT_ADDRESS = "127.0.0.1";
	
	// Error Messages
	private static final String EMPTY_TEXT = "Could not parse Origin Field because text is empty";
	private static final String INVALID_TEXT = "Could not parse Origin Field text: %s";
	
	private String username;
	private int sessionId;
	private int sessionVersion;
	private String netType;
	private String addressType;
	private String address;
	
	public OriginField(String username, int sessionId, int sessionVersion, String netType, String addressType, String address) {
		this.username = username;
		this.sessionId = sessionId;
		this.sessionVersion = sessionVersion;
		this.netType = netType;
		this.addressType = addressType;
		this.address = address;
	}
	
	public OriginField(int sessionId, String address) {
		this(DEFAULT_USERNAME, sessionId, DEFAULT_SESSION_VERSION, DEFAULT_NET_TYPE, DEFAULT_ADDRESS_TYPE, address);
	}
	
	public OriginField() {
		this(DEFAULT_SESSION_ID, DEFAULT_ADDRESS);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	public int getSessionVersion() {
		return sessionVersion;
	}

	public void setSessionVersion(int sessionVersion) {
		this.sessionVersion = sessionVersion;
	}

	public String getNetType() {
		return netType;
	}

	public void setNetType(String netType) {
		this.netType = netType;
	}

	public String getAddressType() {
		return addressType;
	}

	public void setAddressType(String addressType) {
		this.addressType = addressType;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
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
		
		// Extract the value part of the string (remove o=)
		String value = text.substring(2);
		// Split values and store them
		String[] values = value.split(" ");
		try {
			this.username = values[0];
			this.sessionId = Integer.valueOf(values[1]);
			this.sessionVersion = Integer.valueOf(values[2]);
			this.netType = values[3];
			this.addressType = values[4];
			this.address = values[5];
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			throw new SdpException(String.format(INVALID_TEXT, text), e);
		}
	}
	
	@Override
	public String toString() {
		return String.format(FORMAT, this.username, this.sessionId, this.sessionVersion, this.netType, this.addressType, this.address);
	}
	
}
