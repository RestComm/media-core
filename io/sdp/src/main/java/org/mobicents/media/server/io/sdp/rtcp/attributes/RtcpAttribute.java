package org.mobicents.media.server.io.sdp.rtcp.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=rtcp:[port]
 * 
 * <p>
 * The RTCP attribute is used to document the RTCP port used for media stream,
 * when that port is not the next higher (odd) port number following the RTP
 * port described in the media line.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see <a href="https://tools.ietf.org/html/rfc3605">RFC3605</a>
 */
public class RtcpAttribute extends AttributeField {
	
	private static final String NAME = "rtcp";
	private static final int DEFAULT_PORT = 0;
	
	private int port;
	private String networkType;
	private String addressType;
	private String address;
	
	public RtcpAttribute() {
		this(DEFAULT_PORT);
	}
	
	public RtcpAttribute(int port) {
		this(port, null, null, null);
	}

	public RtcpAttribute(int port, String networkType, String addressType, String address) {
		super(NAME);
		this.port = port;
		this.networkType = networkType;
		this.addressType = addressType;
		this.address = address;
	}

	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getNetworkType() {
		return networkType;
	}

	public void setNetworkType(String networkType) {
		this.networkType = networkType;
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
	public String toString() {
		super.builder.setLength(0);
		super.builder.append(BEGIN).append(NAME).append(ATTRIBUTE_SEPARATOR).append(this.port);
		if (this.networkType != null && !this.networkType.isEmpty()) {
			super.builder.append(" ")
			        .append(this.networkType).append(" ")
					.append(this.addressType).append(" ")
					.append(this.address);
		}
		return super.builder.toString();
	}
	
}
