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
	
	public RtcpAttribute() {
		this(DEFAULT_PORT);
	}
	
	public RtcpAttribute(int port) {
		super(NAME);
		this.port = port;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public String toString() {
		super.builder.setLength(0);
		super.builder.append(BEGIN).append(NAME).append(ATTRIBUTE_SEPARATOR).append(this.port);
		return super.builder.toString();
	}
	
}
