package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.exception.SdpException;

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
	private static final String BEGIN = String.valueOf(TYPE) + FIELD_SEPARATOR;
	private static final String FORMAT = BEGIN + "%s %s %s";
	
	// Default values
	private static final String DEFAULT_NET_TYPE = "IN";
	private static final String DEFAULT_ADDRESS_TYPE = "IP4"; 
	private static final String DEFAULT_ADDRESS = "127.0.0.1"; 
	
	// Error Messages
	private static final String EMPTY_TEXT = "Could not parse Connection Field because text is empty";
	private static final String INVALID_TEXT = "Could not parse Connection Field text: %s";
	
	private String networkType;
	private String addressType;
	private String address;
	
	public ConnectionField() {
		this(DEFAULT_NET_TYPE, DEFAULT_ADDRESS_TYPE, DEFAULT_ADDRESS);
	}
	
	public ConnectionField(String netType, String addressType, String address) {
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
	public char getType() {
		return TYPE;
	}

	@Override
	public void parse(String text) throws SdpException {
		if(text == null || text.isEmpty()) {
			throw new SdpException(EMPTY_TEXT);
		}

		if(text.startsWith(BEGIN)) {
			try {
				String[] values = text.substring(2).split(" ");
				this.networkType = values[0];
				this.addressType = values[1];
				this.address = values[2];
			} catch (IndexOutOfBoundsException e) {
				throw new SdpException(String.format(INVALID_TEXT, text), e);
			}
		} else {
			throw new SdpException(String.format(INVALID_TEXT, text));
		}
	}
	
	@Override
	public String toString() {
		return String.format(FORMAT, this.networkType, this.addressType, this.address);
	}

}
