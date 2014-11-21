package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.SdpException;

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
	private static final char TYPE = 'c';
	private static final String BEGIN = TYPE + FIELD_SEPARATOR;
	private static final int BEGIN_LENGTH = BEGIN.length();
	private static final String REGEX = "^"+ BEGIN + "\\w+\\s\\w+\\s[0-9\\.]+$";
	// TODO use proper regex for IP address instead of [0-9\\.]+
	
	// Default values
	private static final String DEFAULT_NET_TYPE = "IN";
	private static final String DEFAULT_ADDRESS_TYPE = "IP4"; 
	private static final String DEFAULT_ADDRESS = "127.0.0.1";
	
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
			this.networkType = values[0];
			this.addressType = values[1];
			this.address = values[2];
		} catch (Exception e) {
			throw new SdpException(String.format(PARSE_ERROR, text), e);
		}
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
