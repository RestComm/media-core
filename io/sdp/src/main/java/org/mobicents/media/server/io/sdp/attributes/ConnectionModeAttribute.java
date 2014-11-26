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

	public ConnectionModeAttribute() {
		this(ConnectionMode.SEND_RECV);
	}

	public ConnectionModeAttribute(ConnectionMode mode) {
		super(mode);
	}

	public void setMode(ConnectionMode mode) {
		super.key = mode.getMode();
		super.connectionMode = mode;
	}

}
