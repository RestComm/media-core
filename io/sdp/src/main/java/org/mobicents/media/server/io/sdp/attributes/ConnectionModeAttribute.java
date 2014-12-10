package org.mobicents.media.server.io.sdp.attributes;

/**
 * a=[inactive|recvonly|sendonly|sendrecv]
 * 
 * <p>
 * Reusable connection mode attribute that allows dynamic overriding of the
 * Connection Mode.
 * </p>
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see {@link InactiveAttribute} {@link RecvOnlyAttribute} {@link SendOnlyAttribute} {@link SendRecvAttribute} 
 */
public class ConnectionModeAttribute extends AbstractConnectionModeAttribute {
	
	public static final String SENDONLY = "sendonly";
	public static final String RECVONLY = "recvonly";
	public static final String SENDRECV = "sendrecv";
	public static final String INACTIVE = "inactive";

	public ConnectionModeAttribute() {
		this(SENDRECV);
	}

	public ConnectionModeAttribute(String mode) {
		super(mode);
	}

	public void setMode(String mode) {
		super.key = mode;
	}
	
}
