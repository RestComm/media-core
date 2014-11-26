package org.mobicents.media.server.io.sdp.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * Fake attribute that gathers recvonly, sendonly, sendrecv.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public abstract class AbstractConnectionModeAttribute extends AttributeField {

	public enum ConnectionMode {
		SEND_ONLY("sendonly"), RECV_ONLY("recvonly"), SEND_RECV("sendrecv"), INACTIVE("inactive");

		private final String mode;

		private ConnectionMode(String mode) {
			this.mode = mode;
		}

		public String getMode() {
			return mode;
		}

		public static ConnectionMode fromMode(String mode) {
			if (mode == null || mode.isEmpty()) {
				return null;
			}

			for (ConnectionMode cm : values()) {
				if (cm.mode.equals(mode)) {
					return cm;
				}
			}
			return null;
		}
		
		public static boolean containsMode(String mode) {
			return fromMode(mode) != null;
		}
	}

	protected ConnectionMode connectionMode;

	protected AbstractConnectionModeAttribute(String name, ConnectionMode mode) {
		super(name);
		this.connectionMode = mode;
	}
	
	protected AbstractConnectionModeAttribute(ConnectionMode mode) {
		this(mode.mode, mode);
	}

	public ConnectionMode getConnectionMode() {
		return connectionMode;
	}

}
