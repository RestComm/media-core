package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;

/**
 * c=[nettype] [addrtype] [connection-address]
 * 
 * <p>
 * The "c=" field contains connection data.<br>
 * A session description MUST contain either at least one "c=" field in each
 * media description or a single "c=" field at the session level. It MAY contain
 * a single session-level "c=" field and additional "c=" field(s) per media
 * description, in which case the per-media values override the session-level
 * settings for the respective media.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectionField implements Field {

	// Parsing
	protected static final char TYPE = 'c';
	protected static final String BEGIN = TYPE + FIELD_SEPARATOR;
	private static final int BEGIN_LENGTH = BEGIN.length();
	
	// Default values
	private static final String DEFAULT_NET_TYPE = "IN";
	private static final String DEFAULT_ADDRESS_TYPE = "IP4"; 
	private static final String DEFAULT_ADDRESS = "0.0.0.0";
	
	private final StringBuilder builder;
	
	private String networkType;
	private String addressType;
	private String address;
	
	public ConnectionField() {
		this(DEFAULT_NET_TYPE, DEFAULT_ADDRESS_TYPE, DEFAULT_ADDRESS);
	}
	
	public ConnectionField(String netType, String addressType, String address) {
		this.builder = new StringBuilder(BEGIN);
		this.networkType = netType;
		this.addressType = addressType;
		this.address = address;
	}
	
	public String getNetworkType() {
		return networkType;
	}

	public void setNetworkType(String netType) {
		this.networkType = netType;
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
	public String toString() {
		// Clean builder
		this.builder.setLength(BEGIN_LENGTH);
		this.builder.append(this.networkType).append(" ")
				.append(this.addressType).append(" ")
				.append(this.address);
		return this.builder.toString();
	}

}
