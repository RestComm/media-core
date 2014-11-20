package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
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
	private static final String BEGIN = String.valueOf(TYPE) + FIELD_SEPARATOR;
	private static final String FORMAT = BEGIN + "%s %d %d %s %s %s";
	// TODO use proper regex for IP address instead of [0-9\\.]+
	private static final String REGEX = "^" + BEGIN + "\\S+\\s\\d+\\s\\d+\\s\\w+\\s\\w+\\s[0-9\\.]+";
	
	// Default values
	private static final String DEFAULT_USERNAME = "-";
	private static final int DEFAULT_SESSION_ID = 0;
	private static final int DEFAULT_SESSION_VERSION = 1;
	private static final String DEFAULT_NET_TYPE = "IN";
	private static final String DEFAULT_ADDRESS_TYPE = "IP4";
	private static final String DEFAULT_ADDRESS = "127.0.0.1";
	
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
			String[] values = text.substring(2).split(" ");
			this.username = values[0];
			this.sessionId = Integer.valueOf(values[1]);
			this.sessionVersion = Integer.valueOf(values[2]);
			this.netType = values[3];
			this.addressType = values[4];
			this.address = values[5];
		} catch (Exception e) {
			throw new SdpException(String.format(PARSE_ERROR, text), e);
		}
	}
	
	@Override
	public String toString() {
		return String.format(FORMAT, this.username, this.sessionId, this.sessionVersion, this.netType, this.addressType, this.address);
	}
	
}
