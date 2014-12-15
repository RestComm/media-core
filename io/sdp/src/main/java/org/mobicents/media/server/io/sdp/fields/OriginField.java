package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.SdpField;

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
public class OriginField implements SdpField {

	// Parsing
	public static final char FIELD_TYPE = 'o';
	private static final String BEGIN = "o=";
	private static final int BEGIN_LEN = BEGIN.length();
	
	// Default values
	private static final String DEFAULT_USERNAME = "-";
	private static final int DEFAULT_SESSION_ID = 0;
	private static final int DEFAULT_SESSION_VERSION = 1;
	private static final String DEFAULT_NET_TYPE = "IN";
	private static final String DEFAULT_ADDRESS_TYPE = "IP4";
	private static final String DEFAULT_ADDRESS = "0.0.0.0";
	
	private final StringBuilder builder;
	
	private String username;
	private long sessionId;
	private int sessionVersion;
	private String netType;
	private String addressType;
	private String address;
	
	public OriginField(String username, long sessionId, int sessionVersion, String netType, String addressType, String address) {
		this.builder = new StringBuilder(BEGIN);
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

	public long getSessionId() {
		return sessionId;
	}

	public void setSessionId(long sessionId) {
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
		return FIELD_TYPE;
	}
	
	@Override
	public String toString() {
		// clear builder
		this.builder.setLength(BEGIN_LEN);
		this.builder.append(this.username).append(" ")
		        .append(this.sessionId).append(" ")
		        .append(this.sessionVersion).append(" ")
		        .append(this.netType).append(" ")
				.append(this.addressType).append(" ")
				.append(this.address);
		return this.builder.toString();
	}
	
}
